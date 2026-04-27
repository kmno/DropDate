package com.kmno.dropdate.data.repository

import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.mapper.ReleaseMapper
import com.kmno.dropdate.data.remote.anilist.AniListApi
import com.kmno.dropdate.data.remote.anilist.aniListScheduleQuery
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
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

class ReleaseRepositoryImpl
    @Inject
    constructor(
        private val dao: ReleaseDao,
        private val tmdbApi: TmdbApi,
        private val tvMazeApi: TvMazeApi,
        private val aniListApi: AniListApi,
        private val mapper: ReleaseMapper,
    ) : ReleaseRepository {
        override fun getReleasesForWeek(
            weekStart: LocalDate,
            weekEnd: LocalDate,
        ): Flow<List<Release>> =
            dao.observeByWeek(weekStart.toString(), weekEnd.toString()).map { entities ->
                entities.map(mapper::toDomain)
            }

        override suspend fun syncReleases(
            weekStart: LocalDate,
            weekEnd: LocalDate,
        ): Result<Unit> =
            runCatching {
                coroutineScope {
                    val dayCount = (weekEnd.toEpochDay() - weekStart.toEpochDay() + 1).toInt()

                    // Fire independent network calls simultaneously
                    val movies =
                        async {
                            tmdbApi.discoverMovies(
                                startDate = weekStart.toString(),
                                endDate = weekEnd.toString(),
                            )
                        }
                    val upcomingMovies =
                        async {
                            tmdbApi.getUpcomingMovies()
                        }

                    val allTvMazeDays =
                        (0 until dayCount)
                            .map { i ->
                                async {
                                    tvMazeApi.getStreamingSchedule(
                                        weekStart.plusDays(i.toLong()).toString(),
                                    )
                                }
                            }.awaitAll()
                            .flatten()

                    val weekStartUnix = weekStart.atStartOfDay().toEpochSecond(ZoneOffset.UTC).toInt()
                    val weekEndUnix =
                        weekEnd.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC).toInt()
                    val anime =
                        async {
                            aniListApi.getAnimeSchedule(
                                aniListScheduleQuery(
                                    weekStartUnix,
                                    weekEndUnix,
                                ),
                            )
                        }

                    val entities =
                        buildList {
                            addAll(
                                mapper.fromTmdb(
                                    movies.await(),
                                    upcomingMovies.await(),
                                ),
                            )
                            addAll(mapper.fromTvMaze(allTvMazeDays))
                            addAll(mapper.fromAniList(anime.await()))
                        }
                    dao.upsertAll(entities)
                }
            }

        override suspend fun deleteOldReleases(before: LocalDate) {
            dao.deleteOldReleases(before.toString())
        }

        override fun searchReleasesTitle(query: String): Flow<List<Release>> =
            dao.searchReleasesTitle(query).map { entities ->
                entities.map(mapper::toDomain)
            }
    }
