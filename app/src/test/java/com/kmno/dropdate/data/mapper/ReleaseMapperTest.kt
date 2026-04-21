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

    @Test
    fun `fromTmdb maps movie with future date to UPCOMING`() {
        val futureDate = LocalDate.now().plusDays(5).toString()
        val dto =
            TmdbMovieListDto(
                results =
                    listOf(
                        TmdbMovieDto(
                            id = 1,
                            title = "Dune 3",
                            posterPath = "/poster.jpg",
                            backdropPath = "/backdrop.jpg",
                            releaseDate = futureDate,
                            voteAverage = 8.5f,
                            overview = "Epic.",
                        ),
                    ),
            )

        val entities = mapper.fromTmdb(dto, TmdbMovieListDto())

        assertEquals(1, entities.size)
        val entity = entities.first()
        assertEquals("tmdb_1", entity.id)
        assertEquals("UPCOMING", entity.status)
        assertEquals(ReleaseType.MOVIE.name, entity.type)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", entity.posterUrl)
        assertEquals(futureDate, entity.airDate)
    }

    @Test
    fun `fromTmdb maps movie with past date to RELEASED`() {
        val pastDate = LocalDate.now().minusDays(5).toString()
        val dto =
            TmdbMovieListDto(
                results =
                    listOf(
                        TmdbMovieDto(
                            id = 2,
                            title = "Old Movie",
                            posterPath = null,
                            backdropPath = null,
                            releaseDate = pastDate,
                            voteAverage = null,
                            overview = null,
                        ),
                    ),
            )

        val entities = mapper.fromTmdb(dto, TmdbMovieListDto())

        assertEquals("RELEASED", entities.first().status)
    }

    @Test
    fun `fromTmdb skips movies with null or blank release date`() {
        val dto =
            TmdbMovieListDto(
                results =
                    listOf(
                        TmdbMovieDto(
                            id = 3,
                            title = "No Date",
                            posterPath = null,
                            backdropPath = null,
                            releaseDate = null,
                            voteAverage = null,
                            overview = null,
                        ),
                    ),
            )

        val entities = mapper.fromTmdb(dto, TmdbMovieListDto())

        assertEquals(0, entities.size)
    }

    @Test
    fun `toDomain maps entity to Release correctly`() {
        val entity =
            ReleaseEntity(
                id = "tmdb_1",
                title = "Dune 3",
                posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/original/backdrop.jpg",
                type = "MOVIE",
                status = "UPCOMING",
                airDate = "2025-08-01",
                airTime = null,
                platform = null,
                episodeLabel = null,
                rating = 8.5f,
                synopsis = "Epic.",
                syncedAt = 0L,
            )

        val domain = mapper.toDomain(entity)

        assertEquals("tmdb_1", domain.id)
        assertEquals(ReleaseType.MOVIE, domain.type)
        assertEquals(ReleaseStatus.UPCOMING, domain.status)
        assertEquals(LocalDate.of(2025, 8, 1), domain.airDate)
        assertNull(domain.airTime)
        assertEquals(8.5f, domain.rating)
    }
}
