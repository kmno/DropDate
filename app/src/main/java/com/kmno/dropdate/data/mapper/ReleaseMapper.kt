package com.kmno.dropdate.data.mapper

// import com.kmno.dropdate.data.remote.trakt.TraktAnticipatedMovieDto
// import com.kmno.dropdate.data.remote.trakt.TraktAnticipatedShowDto
import com.kmno.dropdate.data.local.entity.ReleaseEntity
import com.kmno.dropdate.data.remote.anilist.AniListResponse
import com.kmno.dropdate.data.remote.tmdb.TmdbMovieListDto
import com.kmno.dropdate.data.remote.tvmaze.TvMazeScheduleEntryDto
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import javax.inject.Inject

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"
private const val TMDB_POSTER_SIZE = "w500"
private const val TMDB_BACKDROP_SIZE = "original"

private val TMDB_MOVIE_GENRES =
    mapOf(
        28 to "Action",
        12 to "Adventure",
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        14 to "Fantasy",
        36 to "History",
        27 to "Horror",
        10402 to "Music",
        9648 to "Mystery",
        10749 to "Romance",
        878 to "Sci-Fi",
        10770 to "TV Movie",
        53 to "Thriller",
        10752 to "War",
        37 to "Western",
    )

private val POPULAR_PLATFORMS =
    setOf(
        "netflix",
        "hbo",
        "max",
        "hulu",
        "amazon",
        "prime",
        "amc",
        "showmax",
        "mgm",
        "disney",
        "apple",
        "peacock",
        "paramount",
        "fx",
        "showtime",
        "crunchyroll",
    )

private fun String.isPopularStreamer() = lowercase().let { l -> POPULAR_PLATFORMS.any { l.contains(it) } }

private fun String.isEnglish() = lowercase().let { it == "english" }

private fun String.isScripted() = lowercase() == "scripted"

class ReleaseMapper
    @Inject
    constructor() {
        // ── Movies (TMDB) ────────────────────────────────────────────────────────

        fun fromTmdb(
            movies: TmdbMovieListDto,
            upcomingMovies: TmdbMovieListDto,
        ): List<ReleaseEntity> {
            val seen = mutableSetOf<String>()
            val now = System.currentTimeMillis()

            val movieEntities =
                movies.results
                    .filter { "tmdb_m_${it.id}" !in seen && seen.add("tmdb_m_${it.id}") }
                    .mapNotNull { dto ->
                        val dateStr =
                            dto.releaseDate?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val date = parseLocalDate(dateStr) ?: return@mapNotNull null
                        ReleaseEntity(
                            id = "tmdb_m_${dto.id}",
                            title = dto.title,
                            posterUrl = dto.posterPath?.let { "$TMDB_IMAGE_BASE$TMDB_POSTER_SIZE$it" },
                            backdropUrl = dto.backdropPath?.let { "$TMDB_IMAGE_BASE$TMDB_BACKDROP_SIZE$it" },
                            type = ReleaseType.MOVIE.name,
                            status = releaseStatus(date),
                            airDate = dateStr,
                            premiered = dateStr,
                            airTime = null,
                            platform = null,
                            episodeLabel = null,
                            rating = dto.voteAverage,
                            synopsis = dto.overview,
                            genres =
                                dto.genreIds
                                    .mapNotNull { TMDB_MOVIE_GENRES[it] }
                                    .joinToString(",")
                                    .takeIf { it.isNotBlank() },
                            syncedAt = now,
                        )
                    }

            val uMovieEntities =
                upcomingMovies.results
                    .filter { "tmdb_m_${it.id}" !in seen && seen.add("tmdb_m_${it.id}") }
                    .mapNotNull { dto ->
                        val dateStr =
                            dto.releaseDate?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val date = parseLocalDate(dateStr) ?: return@mapNotNull null
                        ReleaseEntity(
                            id = "tmdb_m_${dto.id}",
                            title = dto.title,
                            posterUrl = dto.posterPath?.let { "$TMDB_IMAGE_BASE$TMDB_POSTER_SIZE$it" },
                            backdropUrl = dto.backdropPath?.let { "$TMDB_IMAGE_BASE$TMDB_BACKDROP_SIZE$it" },
                            type = ReleaseType.MOVIE.name,
                            status = releaseStatus(date),
                            airDate = dateStr,
                            premiered = dateStr,
                            airTime = null,
                            platform = null,
                            episodeLabel = null,
                            rating = dto.voteAverage,
                            synopsis = dto.overview,
                            genres =
                                dto.genreIds
                                    .mapNotNull { TMDB_MOVIE_GENRES[it] }
                                    .joinToString(",")
                                    .takeIf { it.isNotBlank() },
                            syncedAt = now,
                        )
                    }

            /*    val tvEntities =
                    tvShows.results
                        .filter { "tmdb_t_${it.id}" !in seen && seen.add("tmdb_t_${it.id}") }
                        .mapNotNull { dto ->
                            val dateStr =
                                dto.firstAirDate?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val date = parseLocalDate(dateStr) ?: return@mapNotNull null
                            ReleaseEntity(
                                id = "tmdb_t_${dto.id}",
                                title = dto.name,
                                posterUrl = dto.posterPath?.let { "$TMDB_IMAGE_BASE$TMDB_POSTER_SIZE$it" },
                                backdropUrl = dto.backdropPath?.let { "$TMDB_IMAGE_BASE$TMDB_BACKDROP_SIZE$it" },
                                type = ReleaseType.SERIES.name,
                                status = releaseStatus(date),
                                airDate = dateStr,
                                airTime = null,
                                platform = null,
                                episodeLabel = null,
                                rating = dto.voteAverage,
                                synopsis = dto.overview,
                                genres =
                                    dto.genreIds
                                        .mapNotNull { TMDB_TV_GENRES[it] }
                                        .joinToString(",")
                                        .takeIf { it.isNotBlank() },
                                syncedAt = now,
                            )
                        }*/
            return movieEntities + uMovieEntities
        }

        // ── TV Series (TVMaze) ──────────────────────────────────────────────────

        fun fromTvMaze(entries: List<TvMazeScheduleEntryDto>): List<ReleaseEntity> {
            val now = System.currentTimeMillis()
            return entries
                .filter { entry ->
                    val show = entry.show ?: return@filter false
                    val isPopular = (
                        show.network?.name?.isPopularStreamer() == true ||
                            show.webChannel?.name?.isPopularStreamer() == true
                    )
                    val isEnglish = show.language?.isEnglish() == true
                    val isScripted = show.type.isScripted()
                    isPopular && isEnglish && isScripted
                }
                // Group by show + airdate: same-day multi-episode drops become one row
                .groupBy { "${it.show!!.id}_${it.airdate}" }
                .mapNotNull { (_, group) ->
                    val first = group.first()
                    val show = first.show ?: return@mapNotNull null
                    val dateStr = first.airdate ?: return@mapNotNull null
                    val date = parseLocalDate(dateStr) ?: return@mapNotNull null

                    val episodeLabel =
                        if (group.size == 1) {
                            if (first.season != null && first.number != null) {
                                "S%02d · E%02d".format(first.season, first.number)
                            } else {
                                null
                            }
                        } else {
                            val season = group.mapNotNull { it.season }.distinct().singleOrNull()
                            if (season != null) {
                                "S$season · ${group.size} eps (all)"
                            } else {
                                "${group.size} eps (all)"
                            }
                        }

                    ReleaseEntity(
                        id = "tvmaze_${show.id}_$dateStr",
                        title = show.name,
                        posterUrl = show.image?.original ?: show.image?.medium,
                        backdropUrl = show.image?.original ?: show.image?.medium,
                        type = ReleaseType.SERIES.name,
                        status = releaseStatus(date),
                        airDate = dateStr,
                        premiered = show.premiered,
                        airTime = if (group.size == 1) first.airtime else null,
                        platform = show.network?.name ?: show.webChannel?.name,
                        episodeLabel = episodeLabel,
                        rating = show.rating?.average?.let { it * 1f },
                        synopsis = show.summary?.replace(Regex("<.*?>"), ""),
                        genres = show.genres.joinToString(",").takeIf { it.isNotBlank() },
                        syncedAt = now,
                    )
                }
        }

    /*
        // ── Anticipated (Trakt + TMDB images) ───────────────────────────────────
        // Images are fetched by the repository using TMDB IDs from Trakt IDs,
        // then passed here as maps to avoid N+1 API calls in the mapper.

        fun fromTrakt(
            movies: List<TraktAnticipatedMovieDto>,
            shows: List<TraktAnticipatedShowDto>,
            movieImages: Map<Int, TmdbImagesDto>,
            showImages: Map<Int, TmdbImagesDto>,
        ): List<ReleaseEntity> {
            val now = System.currentTimeMillis()
            val movieEntities = movies.mapNotNull { item ->
                val movie = item.movie ?: return@mapNotNull null
                val traktId = movie.ids?.trakt ?: return@mapNotNull null
                val dateStr = movie.released?.take(10)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val date = parseLocalDate(dateStr) ?: return@mapNotNull null
                val images = movieImages[movie.ids.tmdb]
                ReleaseEntity(
                    id = "trakt_m_$traktId",
                    title = movie.title,
                    posterUrl = images?.posterPath?.let { "$TMDB_IMAGE_BASE$TMDB_POSTER_SIZE$it" },
                    backdropUrl = images?.backdropPath?.let { "$TMDB_IMAGE_BASE$TMDB_BACKDROP_SIZE$it" },
                    type = ReleaseType.MOVIE.name,
                    status = releaseStatus(date),
                    airDate = dateStr,
                    airTime = null,
                    platform = null,
                    episodeLabel = null,
                    rating = movie.rating,
                    synopsis = movie.overview,
                    syncedAt = now,
                )
            }
            val showEntities = shows.mapNotNull { item ->
                val show = item.show ?: return@mapNotNull null
                val traktId = show.ids?.trakt ?: return@mapNotNull null
                val dateStr = show.firstAired?.take(10)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val date = parseLocalDate(dateStr) ?: return@mapNotNull null
                val images = showImages[show.ids.tmdb]
                ReleaseEntity(
                    id = "trakt_s_$traktId",
                    title = show.title,
                    posterUrl = images?.posterPath?.let { "$TMDB_IMAGE_BASE$TMDB_POSTER_SIZE$it" },
                    backdropUrl = images?.backdropPath?.let { "$TMDB_IMAGE_BASE$TMDB_BACKDROP_SIZE$it" },
                    type = ReleaseType.SERIES.name,
                    status = releaseStatus(date),
                    airDate = dateStr,
                    airTime = null,
                    platform = null,
                    episodeLabel = null,
                    rating = show.rating,
                    synopsis = show.overview,
                    syncedAt = now,
                )
            }
            return movieEntities + showEntities
        }
     */

        // ── Anime (AniList airingSchedules) ─────────────────────────────────────

        fun fromAniList(response: AniListResponse): List<ReleaseEntity> {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            return response.data
                ?.page
                ?.airingSchedules
                .orEmpty()
                .mapNotNull { entry ->
                    val media = entry.media ?: return@mapNotNull null
                    val title =
                        media.title?.english?.takeIf { it.isNotBlank() }
                            ?: media.title?.romaji
                            ?: return@mapNotNull null
                    val date =
                        Instant
                            .ofEpochSecond(entry.airingAt)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    val dateStr = date.toString()
                    val status =
                        if (date.isAfter(today)) {
                            ReleaseStatus.UPCOMING.name
                        } else {
                            ReleaseStatus.RELEASED.name
                        }
                    val platform =
                        media.externalLinks
                            .firstOrNull { it.site?.isPopularStreamer() == true }
                            ?.site
                    ReleaseEntity(
                        id = "anilist_${media.id}_${dateStr}_ep${entry.episode}",
                        title = title,
                        posterUrl = media.coverImage?.large,
                        backdropUrl = media.coverImage?.large,
                        type = ReleaseType.ANIME.name,
                        status = status,
                        airDate = dateStr,
                        premiered = dateStr,
                        airTime = null,
                        platform = platform,
                        episodeLabel = "Ep ${entry.episode}",
                        rating = media.averageScore?.let { it / 10f },
                        synopsis = media.description?.replace(Regex("<.*?>"), ""),
                        genres =
                            media.genres
                                .map { genre ->
                                    if (genre == "Hentai") "$genre \uD83D\uDD1E" else genre
                                }.joinToString(",")
                                .takeIf { it.isNotBlank() },
                        syncedAt = now,
                    )
                }
        }

        // ── Domain mapping ───────────────────────────────────────────────────────

        fun toDomain(entity: ReleaseEntity): Release =
            Release(
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
                genres = entity.genres?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            )

        // ── Helpers ──────────────────────────────────────────────────────────────

        private fun releaseStatus(date: LocalDate) =
            if (date.isAfter(LocalDate.now())) ReleaseStatus.UPCOMING.name else ReleaseStatus.RELEASED.name

        private fun parseLocalDate(value: String): LocalDate? =
            try {
                LocalDate.parse(value.take(10))
            } catch (_: DateTimeParseException) {
                null
            }
    }
