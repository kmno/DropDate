package com.kmno.dropdate.data.repository

import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.mapper.ReleaseMapper
import com.kmno.dropdate.data.remote.animeschedule.AnimeScheduleApi
import com.kmno.dropdate.data.remote.jikan.JikanApi
import com.kmno.dropdate.data.remote.tmdb.TmdbApi
import com.kmno.dropdate.data.remote.tvmaze.TvMazeApi
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class ReleaseRepositoryImpl @Inject constructor(
    private val dao: ReleaseDao,
    private val tmdbApi: TmdbApi,
    private val tvMazeApi: TvMazeApi,
    private val jikanApi: JikanApi,
    private val animeScheduleApi: AnimeScheduleApi,
    private val mapper: ReleaseMapper,
) : ReleaseRepository {

    override fun getReleasesForWeek(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Flow<List<Release>> =
        dao.observeByWeek(weekStart.toString(), weekEnd.toString())
            .map { entities -> entities.map(mapper::toDomain) }

    override suspend fun syncReleases(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Result<Unit> = runCatching {
        coroutineScope {
            val upcoming      = async { tmdbApi.getUpcomingMovies() }
            val popular       = async { tmdbApi.getPopularMovies() }
            val streaming     = async { tvMazeApi.getStreamingSchedule(weekStart.toString()) }
            val broadcast     = async { tvMazeApi.getBroadcastSchedule(weekStart.toString()) }
            val airingAnime   = async { jikanApi.getCurrentlyAiringAnime() }
            val upcomingAnime = async { jikanApi.getUpcomingAnime() }
            val timetable     = async { animeScheduleApi.getTimetable() }

            val entities = buildList {
                addAll(mapper.fromTmdb(upcoming.await(), popular.await()))
                addAll(mapper.fromTvMaze(streaming.await(), broadcast.await()))
                addAll(mapper.fromJikan(airingAnime.await(), upcomingAnime.await(), timetable.await()))
            }

            dao.upsertAll(entities)
            dao.deleteStale(System.currentTimeMillis() - 7.days.inWholeMilliseconds)
        }
    }
}
