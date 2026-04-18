package com.kmno.dropdate.data.mapper

import com.kmno.dropdate.data.local.entity.ReleaseEntity
import com.kmno.dropdate.data.remote.animeschedule.AnimeScheduleEntryDto
import com.kmno.dropdate.data.remote.jikan.JikanAnimeDto
import com.kmno.dropdate.data.remote.jikan.JikanSeasonDto
import com.kmno.dropdate.data.remote.tmdb.TmdbMovieListDto
import com.kmno.dropdate.data.remote.tvmaze.TvMazeScheduleEntryDto
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException
import javax.inject.Inject

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"
private const val TMDB_POSTER_SIZE = "w500"
private const val TMDB_BACKDROP_SIZE = "original"
private const val ANIME_SCHEDULE_IMAGE_BASE = "https://animeschedule.net/img/shows/"

class ReleaseMapper @Inject constructor() {

    fun fromTmdb(upcoming: TmdbMovieListDto, popular: TmdbMovieListDto): List<ReleaseEntity> {
        val seen = mutableSetOf<Int>()
        val now = System.currentTimeMillis()
        return (upcoming.results + popular.results)
            .filter { it.id !in seen && seen.add(it.id) }
            .mapNotNull { dto ->
                val dateStr = dto.releaseDate?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val date = parseLocalDate(dateStr) ?: return@mapNotNull null
                ReleaseEntity(
                    id = "tmdb_${dto.id}",
                    title = dto.title,
                    posterUrl = dto.posterPath?.let { "$TMDB_IMAGE_BASE$TMDB_POSTER_SIZE$it" },
                    backdropUrl = dto.backdropPath?.let { "$TMDB_IMAGE_BASE$TMDB_BACKDROP_SIZE$it" },
                    type = ReleaseType.MOVIE.name,
                    status = if (date.isAfter(LocalDate.now())) ReleaseStatus.UPCOMING.name else ReleaseStatus.RELEASED.name,
                    airDate = dateStr,
                    airTime = null,
                    platform = null,
                    episodeLabel = null,
                    rating = dto.voteAverage,
                    synopsis = dto.overview,
                    syncedAt = now,
                )
            }
    }

    fun fromTvMaze(
        streaming: List<TvMazeScheduleEntryDto>,
        broadcast: List<TvMazeScheduleEntryDto>,
    ): List<ReleaseEntity> {
        val seen = mutableSetOf<String>()
        val now = System.currentTimeMillis()
        return (streaming + broadcast)
            .mapNotNull { entry ->
                val show = entry.show ?: return@mapNotNull null
                val dateStr = entry.airdate?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val date = parseLocalDate(dateStr) ?: return@mapNotNull null
                val epLabel = entry.season?.let { s -> entry.number?.let { e -> "S%02dE%02d".format(s, e) } }
                val id = "tvmaze_${show.id}_${epLabel ?: dateStr}"
                if (id in seen || !seen.add(id)) return@mapNotNull null
                val platform = (show.webChannel ?: show.network)?.name
                ReleaseEntity(
                    id = id,
                    title = show.name,
                    posterUrl = show.image?.medium,
                    backdropUrl = show.image?.original,
                    type = ReleaseType.SERIES.name,
                    status = if (date.isAfter(LocalDate.now())) ReleaseStatus.UPCOMING.name else ReleaseStatus.RELEASED.name,
                    airDate = dateStr,
                    airTime = entry.airtime?.takeIf { it.isNotBlank() },
                    platform = platform,
                    episodeLabel = epLabel,
                    rating = show.rating?.average,
                    synopsis = show.summary?.replace(Regex("<.*?>"), ""),
                    syncedAt = now,
                )
            }
    }

    fun fromJikan(
        airing: JikanSeasonDto,
        upcoming: JikanSeasonDto,
        timetable: List<AnimeScheduleEntryDto>,
    ): List<ReleaseEntity> {
        val timetableMap = timetable.associateBy { it.route }
        val seen = mutableSetOf<Int>()
        val now = System.currentTimeMillis()
        return (airing.data + upcoming.data)
            .filter { it.malId !in seen && seen.add(it.malId) }
            .mapNotNull { anime -> mapJikanAnime(anime, timetableMap, now) }
    }

    private fun mapJikanAnime(
        anime: JikanAnimeDto,
        timetableMap: Map<String, AnimeScheduleEntryDto>,
        now: Long,
    ): ReleaseEntity? {
        val dateStr = anime.aired?.from?.take(10)?.takeIf { it.isNotBlank() } ?: return null
        val date = parseLocalDate(dateStr) ?: return null
        val schedule = timetableMap.values.find { it.title.equals(anime.title, ignoreCase = true) }
        val platform = schedule?.streams?.firstOrNull()?.name
        val airTime = schedule?.episodeTime?.takeIf { it.isNotBlank() }
            ?: anime.broadcast?.time?.takeIf { it.isNotBlank() }
        return ReleaseEntity(
            id = "jikan_${anime.malId}",
            title = anime.title,
            posterUrl = anime.images?.jpg?.largeImageUrl,
            backdropUrl = schedule?.imageRoute?.let { "$ANIME_SCHEDULE_IMAGE_BASE$it" },
            type = ReleaseType.ANIME.name,
            status = if (date.isAfter(LocalDate.now())) ReleaseStatus.UPCOMING.name else ReleaseStatus.RELEASED.name,
            airDate = dateStr,
            airTime = airTime,
            platform = platform,
            episodeLabel = null,
            rating = anime.score,
            synopsis = anime.synopsis,
            syncedAt = now,
        )
    }

    fun toDomain(entity: ReleaseEntity): Release = Release(
        id = entity.id,
        title = entity.title,
        posterUrl = entity.posterUrl,
        backdropUrl = entity.backdropUrl,
        type = ReleaseType.valueOf(entity.type),
        status = ReleaseStatus.valueOf(entity.status),
        airDate = LocalDate.parse(entity.airDate),
        airTime = entity.airTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        platform = entity.platform,
        episodeLabel = entity.episodeLabel,
        rating = entity.rating,
        synopsis = entity.synopsis,
    )

    private fun parseLocalDate(value: String): LocalDate? = try {
        LocalDate.parse(value.take(10))
    } catch (e: DateTimeParseException) {
        null
    }
}
