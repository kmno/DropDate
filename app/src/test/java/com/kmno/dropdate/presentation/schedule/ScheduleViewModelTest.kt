package com.kmno.dropdate.presentation.schedule

import com.kmno.dropdate.core.analytics.AnalyticsHelper
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.domain.usecase.CleanupReleasesUseCase
import com.kmno.dropdate.domain.usecase.GetWeekReleasesUseCase
import com.kmno.dropdate.domain.usecase.SearchReleasesUseCase
import com.kmno.dropdate.domain.usecase.SyncReleasesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val getWeekReleases: GetWeekReleasesUseCase = mock()
    private val syncReleases: SyncReleasesUseCase = mock()
    private val cleanupReleases: CleanupReleasesUseCase = mock()
    private val searchReleasesUseCase: SearchReleasesUseCase = mock()
    private val analyticsHelper: AnalyticsHelper = mock()

    private fun fakeRelease(
        id: String,
        type: ReleaseType,
        date: LocalDate,
    ) = Release(
        id = id,
        seriesId = id,
        title = "T$id",
        posterUrl = null,
        backdropUrl = null,
        type = type,
        status = ReleaseStatus.UPCOMING,
        airDate = date,
        airTime = null,
        platform = null,
        episodeLabel = null,
        rating = null,
        synopsis = null,
    )

    private fun makeViewModel() =
        ScheduleViewModel(
            getWeekReleases,
            syncReleases,
            cleanupReleases,
            searchReleasesUseCase,
            analyticsHelper,
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(getWeekReleases(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(searchReleasesUseCase(any())).thenReturn(flowOf(emptyList()))
        runBlocking { whenever(syncReleases(any(), any())).thenReturn(Result.success(Unit)) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct defaults`() =
        runTest {
            val vm = makeViewModel()
            val state = vm.state.value
            assertEquals(ContentFilter.ALL, state.activeFilter)
            assertNull(state.selectedRelease)
            assertNull(state.error)
        }

    @Test
    fun `onFilterChanged updates activeFilter`() =
        runTest {
            val vm = makeViewModel()
            vm.onFilterChanged(ContentFilter.ANIME)
            assertEquals(ContentFilter.ANIME, vm.state.value.activeFilter)
        }

    @Test
    fun `onReleaseSelected sets selectedRelease`() =
        runTest {
            val vm = makeViewModel()
            val release = fakeRelease("1", ReleaseType.MOVIE, LocalDate.now())
            vm.onReleaseSelected(release)
            assertEquals(release, vm.state.value.selectedRelease)
        }

    @Test
    fun `onSheetDismissed clears selectedRelease`() =
        runTest {
            val vm = makeViewModel()
            val release = fakeRelease("1", ReleaseType.MOVIE, LocalDate.now())
            vm.onReleaseSelected(release)
            vm.onSheetDismissed()
            assertNull(vm.state.value.selectedRelease)
        }

    @Test
    fun `onDaySelected updates selectedDay`() =
        runTest {
            val vm = makeViewModel()
            val tuesday = LocalDate.now().with(java.time.DayOfWeek.TUESDAY)
            vm.onDaySelected(tuesday)
            assertEquals(tuesday, vm.state.value.selectedDay)
        }
}
