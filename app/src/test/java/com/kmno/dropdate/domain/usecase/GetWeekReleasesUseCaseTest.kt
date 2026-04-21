package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class GetWeekReleasesUseCaseTest {
    private val repository: ReleaseRepository = mock()
    private val useCase = GetWeekReleasesUseCase(repository)

    private val monday = LocalDate.of(2025, 6, 9)
    private val sunday = monday.plusDays(6)

    private fun fakeRelease(
        id: String,
        date: LocalDate,
    ) = Release(
        id = id,
        title = "Title $id",
        posterUrl = null,
        backdropUrl = null,
        type = ReleaseType.MOVIE,
        status = ReleaseStatus.UPCOMING,
        airDate = date,
        airTime = null,
        platform = null,
        episodeLabel = null,
        rating = null,
        synopsis = null,
    )

    @Test
    fun `invoke returns flow from repository`() =
        runTest {
            val releases = listOf(fakeRelease("1", monday), fakeRelease("2", sunday))
            whenever(repository.getReleasesForWeek(monday, sunday)).thenReturn(flowOf(releases))

            val result = useCase(monday, sunday).first()

            assertEquals(releases, result)
        }

    @Test
    fun `invoke returns empty list when repository emits empty`() =
        runTest {
            whenever(repository.getReleasesForWeek(monday, sunday)).thenReturn(flowOf(emptyList()))

            val result = useCase(monday, sunday).first()

            assertEquals(emptyList<Release>(), result)
        }
}
