package com.kmno.dropdate.data.mapper

import com.kmno.dropdate.data.local.entity.ReleaseEntity
import com.kmno.dropdate.data.remote.tmdb.TmdbMovieDto
import com.kmno.dropdate.data.remote.tmdb.TmdbMovieListDto
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReleaseMapperTest {
    private val mapper = ReleaseMapper()

    private fun tmdbDto(
        id: Int,
        title: String,
        releaseDate: String?,
    ) = TmdbMovieDto(
        id = id,
        title = title,
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        releaseDate = releaseDate,
        voteAverage = 8.5f,
        overview = "Overview",
        genreIds = emptyList(),
    )

    @Test
    fun `fromTmdb maps movie with future date to UPCOMING`() {
        val futureDate = LocalDate.now().plusDays(5).toString()
        val dto = TmdbMovieListDto(results = listOf(tmdbDto(1, "Dune 3", futureDate)))

        val entities = mapper.fromTmdb(dto, TmdbMovieListDto())

        assertEquals(1, entities.size)
        val entity = entities.first()
        assertEquals("tmdb_m_1", entity.id)
        assertEquals("tmdb_m_1", entity.seriesId)
        assertEquals("UPCOMING", entity.status)
        assertEquals(ReleaseType.MOVIE.name, entity.type)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", entity.posterUrl)
        assertEquals(futureDate, entity.airDate)
    }

    @Test
    fun `fromTmdb maps movie with past date to RELEASED`() {
        val pastDate = LocalDate.now().minusDays(5).toString()
        val dto = TmdbMovieListDto(results = listOf(tmdbDto(2, "Old Movie", pastDate)))

        val entities = mapper.fromTmdb(dto, TmdbMovieListDto())

        assertEquals("RELEASED", entities.first().status)
    }

    @Test
    fun `fromTmdb skips movies with null release date`() {
        val dto = TmdbMovieListDto(results = listOf(tmdbDto(3, "No Date", null)))

        val entities = mapper.fromTmdb(dto, TmdbMovieListDto())

        assertEquals(0, entities.size)
    }

    @Test
    fun `toDomain maps entity to Release correctly with isTracked false by default`() {
        val entity =
            ReleaseEntity(
                id = "tmdb_m_1",
                seriesId = "tmdb_m_1",
                title = "Dune 3",
                posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/original/backdrop.jpg",
                type = "MOVIE",
                status = "UPCOMING",
                premiered = "2025-08-01",
                airDate = "2025-08-01",
                airTime = null,
                platform = null,
                episodeLabel = null,
                rating = 8.5f,
                synopsis = "Epic.",
                syncedAt = 0L,
            )

        val domain = mapper.toDomain(entity)

        assertEquals("tmdb_m_1", domain.id)
        assertEquals("tmdb_m_1", domain.seriesId)
        assertEquals(false, domain.isTracked)
        assertEquals(ReleaseType.MOVIE, domain.type)
        assertEquals(ReleaseStatus.UPCOMING, domain.status)
        assertEquals(LocalDate.of(2025, 8, 1), domain.airDate)
        assertNull(domain.airTime)
        assertEquals(8.5f, domain.rating)
    }

    @Test
    fun `toDomain maps isTracked true when provided`() {
        val entity =
            ReleaseEntity(
                id = "tvmaze_42_2025-08-01",
                seriesId = "tvmaze_42",
                title = "Breaking Bad S2",
                posterUrl = null,
                backdropUrl = null,
                type = "SERIES",
                status = "UPCOMING",
                premiered = "2025-08-01",
                airDate = "2025-08-01",
                airTime = null,
                platform = "Netflix",
                episodeLabel = "S02 · E01",
                rating = 9.5f,
                synopsis = null,
                syncedAt = 0L,
            )

        val domain = mapper.toDomain(entity, isTracked = true)

        assertEquals("tvmaze_42", domain.seriesId)
        assertEquals(true, domain.isTracked)
    }
}
