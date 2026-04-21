package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class SyncReleasesUseCaseTest {
    private val repository: ReleaseRepository = mock()
    private val useCase = SyncReleasesUseCase(repository)

    private val monday = LocalDate.of(2025, 6, 9)
    private val sunday = monday.plusDays(6)

    @Test
    fun `invoke delegates to repository and returns success`() =
        runTest {
            whenever(repository.syncReleases(monday, sunday)).thenReturn(Result.success(Unit))

            val result = useCase(monday, sunday)

            verify(repository).syncReleases(monday, sunday)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `invoke returns failure when repository throws`() =
        runTest {
            whenever(repository.syncReleases(monday, sunday))
                .thenReturn(Result.failure(RuntimeException("network error")))

            val result = useCase(monday, sunday)

            assertTrue(result.isFailure)
            assertEquals("network error", result.exceptionOrNull()?.message)
        }
}
