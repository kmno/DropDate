package com.kmno.dropdate.data.repository

import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.mapper.ReleaseMapper
import com.kmno.dropdate.data.remote.anilist.ANILIST_ANIME_QUERY
import com.kmno.dropdate.data.remote.anilist.AniListApi
import com.kmno.dropdate.data.remote.tmdb.TMDB_POPULAR_PROVIDERS
import com.kmno.dropdate.data.remote.tmdb.TmdbApi
import com.kmno.dropdate.data.remote.tvmaze.TvMazeApi
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class ReleaseRepositoryImpl @Inject constructor(
    private val dao: ReleaseDao,
    private val tmdbApi: TmdbApi,
    private val tvMazeApi: TvMazeApi,
    private val aniListApi: AniListApi,
    private val mapper: ReleaseMapper,
) : ReleaseRepository {

    override fun getReleasesForWeek(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Flow<List<Release>> = dao.observeByWeek(weekStart.toString(), weekEnd.toString())
        .map { entities ->
            entities.map(mapper::toDomain)
        }

    override suspend fun syncReleases(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Result<Unit> = runCatching {
        coroutineScope {
            // Fire independent network calls simultaneously
            val movies = async {
                tmdbApi.discoverMovies(
                    startDate = weekStart.toString(),
                    endDate = weekEnd.toString(),
                )
            }
            val upcomingMovies = async {
                tmdbApi.getUpcomingMovies()
            }
            val tvShows = async {
                tmdbApi.discoverTv(
                    startDate = weekStart.toString(),
                    endDate = weekEnd.toString(),
                    watchProviders = TMDB_POPULAR_PROVIDERS
                )
            }

            // TVMaze: rate limit allows 5 realtime calls; batch first 5 then delay for remaining 2
            val tvMazeBatch1 = (0..14).map { i ->
                async { tvMazeApi.getStreamingSchedule(weekStart.plusDays(i.toLong()).toString()) }
            }
            //val batch1Results = tvMazeBatch1.awaitAll()
            //delay(1_000L)
            // val tvMazeBatch2 = (5..6).map { i ->
            // async { tvMazeApi.getStreamingSchedule(weekStart.plusDays(i.toLong()).toString()) }
            // }
            val allTvMazeDays = tvMazeBatch1.awaitAll().flatten()

            val anime = async { aniListApi.getAnimeSchedule(ANILIST_ANIME_QUERY) }

            val entities = buildList {
                addAll(
                    mapper.fromTmdb(
                        movies.await(),
                        upcomingMovies.await(),
                        tvShows.await(),
                    )
                )
                addAll(mapper.fromTvMaze(allTvMazeDays))
                // addAll(mapper.fromTvMaze(tvMazeUS.await()))
                addAll(mapper.fromAniList(anime.await()))
            }
            dao.upsertAll(entities)
            // dao.deleteStale(System.currentTimeMillis() - 7.days.inWholeMilliseconds)
        }
    }
}
