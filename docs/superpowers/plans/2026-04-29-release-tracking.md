# Release Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow users to track upcoming releases (movies, series, anime) at the series level — tracking persists across weekly syncs, and new episodes of a tracked series appear as tracked automatically.

**Architecture:** A separate `tracked_series` Room table stores tracked `seriesId`s. A `seriesId` field is added to `ReleaseEntity` to group episodes. The repository `combine`s week/tracked queries with the tracked-IDs flow to derive `isTracked` reactively. Business rule (only UPCOMING can be tracked) lives in `SetTrackingUseCase`.

**Tech Stack:** Kotlin, Room (with migration), Hilt (`@Inject constructor`), Kotlin Flows (`combine`), Mockito-Kotlin + JUnit 4 for unit tests.

---

## File Map

| File | Action |
|------|--------|
| `data/local/entity/TrackedSeriesEntity.kt` | **Create** — new Room entity |
| `data/local/entity/ReleaseEntity.kt` | **Modify** — add `seriesId: String` |
| `data/local/AppDatabase.kt` | **Modify** — register new entity, bump to v2, add migration |
| `data/local/dao/ReleaseDao.kt` | **Modify** — add 4 tracking DAO methods |
| `di/DatabaseModule.kt` | **Modify** — wire migration into Room builder |
| `data/mapper/ReleaseMapper.kt` | **Modify** — extract `seriesId` in each source mapper; add `isTracked` param to `toDomain` |
| `domain/model/Release.kt` | **Modify** — add `seriesId: String` and `isTracked: Boolean = false` |
| `domain/repository/ReleaseRepository.kt` | **Modify** — add `setTracking` and `getTrackedReleases` |
| `data/repository/ReleaseRepositoryImpl.kt` | **Modify** — implement new methods; update `getReleasesForWeek` to use `combine` |
| `domain/usecase/SetTrackingUseCase.kt` | **Create** — business rule enforcement |
| `domain/usecase/GetTrackedReleasesUseCase.kt` | **Create** — pass-through use case |
| `data/mapper/ReleaseMapperTest.kt` | **Modify** — fix pre-existing field omissions; add `seriesId` assertions |
| `domain/usecase/GetWeekReleasesUseCaseTest.kt` | **Modify** — add `seriesId` to `fakeRelease` helper |
| `domain/usecase/SetTrackingUseCaseTest.kt` | **Create** — full use case tests |
| `domain/usecase/GetTrackedReleasesUseCaseTest.kt` | **Create** — full use case tests |

---

## Task 1: DB Schema — `TrackedSeriesEntity`, `seriesId` on `ReleaseEntity`, migration

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/data/local/entity/TrackedSeriesEntity.kt`
- Modify: `app/src/main/java/com/kmno/dropdate/data/local/entity/ReleaseEntity.kt`
- Modify: `app/src/main/java/com/kmno/dropdate/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/kmno/dropdate/di/DatabaseModule.kt`

- [ ] **Step 1: Create `TrackedSeriesEntity`**

Create `app/src/main/java/com/kmno/dropdate/data/local/entity/TrackedSeriesEntity.kt`:

```kotlin
package com.kmno.dropdate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_series")
data class TrackedSeriesEntity(
    @PrimaryKey val seriesId: String,
)
```

- [ ] **Step 2: Add `seriesId` to `ReleaseEntity`**

Replace the full file `app/src/main/java/com/kmno/dropdate/data/local/entity/ReleaseEntity.kt`:

```kotlin
package com.kmno.dropdate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "releases")
data class ReleaseEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: String,
    val status: String,
    val premiered: String,
    val airDate: String,
    val airTime: String?,
    val platform: String?,
    val episodeLabel: String?,
    val rating: Float?,
    val synopsis: String?,
    val genres: String? = null,
    val syncedAt: Long,
)
```

- [ ] **Step 3: Update `AppDatabase` — register entity, bump version, define migration**

Replace the full file `app/src/main/java/com/kmno/dropdate/data/local/AppDatabase.kt`:

```kotlin
package com.kmno.dropdate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.local.entity.ReleaseEntity
import com.kmno.dropdate.data.local.entity.TrackedSeriesEntity

@Database(
    entities = [ReleaseEntity::class, TrackedSeriesEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun releaseDao(): ReleaseDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE releases ADD COLUMN seriesId TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tracked_series` " +
                        "(`seriesId` TEXT NOT NULL, PRIMARY KEY(`seriesId`))",
                )
            }
        }
    }
}
```

- [ ] **Step 4: Wire migration into `DatabaseModule`**

In `app/src/main/java/com/kmno/dropdate/di/DatabaseModule.kt`, add `.addMigrations(AppDatabase.MIGRATION_1_2)` to the Room builder:

```kotlin
package com.kmno.dropdate.di

import android.content.Context
import androidx.room.Room
import com.kmno.dropdate.data.local.AppDatabase
import com.kmno.dropdate.data.local.dao.ReleaseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "dropdate.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides @Singleton
    fun provideReleaseDao(db: AppDatabase): ReleaseDao = db.releaseDao()
}
```

- [ ] **Step 5: Build to verify schema compiles**

```bash
gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL. A new schema file appears at `app/schemas/com.kmno.dropdate.data.local.AppDatabase/2.json`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/data/local/entity/TrackedSeriesEntity.kt
git add app/src/main/java/com/kmno/dropdate/data/local/entity/ReleaseEntity.kt
git add app/src/main/java/com/kmno/dropdate/data/local/AppDatabase.kt
git add app/src/main/java/com/kmno/dropdate/di/DatabaseModule.kt
git add app/schemas/
git commit -m "feat: add seriesId to ReleaseEntity and tracked_series table with migration"
```

---

## Task 2: Update `ReleaseMapper` — extract `seriesId`, update `toDomain`

**Files:**
- Modify: `app/src/main/java/com/kmno/dropdate/data/mapper/ReleaseMapper.kt`
- Modify: `app/src/test/java/com/kmno/dropdate/data/mapper/ReleaseMapperTest.kt`

- [ ] **Step 1: Update `fromTmdb` to set `seriesId`**

In `ReleaseMapper.fromTmdb`, add `seriesId = "tmdb_m_${dto.id}"` to both `ReleaseEntity` constructor calls (the `movieEntities` and `uMovieEntities` blocks). The `seriesId` equals the `id` for movies since each movie is its own series:

```kotlin
ReleaseEntity(
    id = "tmdb_m_${dto.id}",
    seriesId = "tmdb_m_${dto.id}",
    title = dto.title,
    // ... rest unchanged
)
```

Apply the same change to both `mapNotNull` blocks in `fromTmdb`.

- [ ] **Step 2: Update `fromTvMaze` to set `seriesId`**

In `ReleaseMapper.fromTvMaze`, inside the `mapNotNull` block, add `seriesId = "tvmaze_${show.id}"` to the `ReleaseEntity` constructor:

```kotlin
ReleaseEntity(
    id = "tvmaze_${show.id}_$dateStr",
    seriesId = "tvmaze_${show.id}",
    title = show.name,
    // ... rest unchanged
)
```

- [ ] **Step 3: Update `fromAniList` to set `seriesId`**

In `ReleaseMapper.fromAniList`, inside the `mapNotNull` block, add `seriesId = "anilist_${media.id}"` to the `ReleaseEntity` constructor:

```kotlin
ReleaseEntity(
    id = "anilist_${media.id}_${dateStr}_ep${entry.episode}",
    seriesId = "anilist_${media.id}",
    title = title,
    // ... rest unchanged
)
```

- [ ] **Step 4: Update `toDomain` to accept and forward `isTracked`**

Replace the `toDomain` function in `ReleaseMapper`:

```kotlin
fun toDomain(entity: ReleaseEntity, isTracked: Boolean = false): Release =
    Release(
        id = entity.id,
        seriesId = entity.seriesId,
        isTracked = isTracked,
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
```

- [ ] **Step 5: Fix and extend `ReleaseMapperTest`**

Replace the full file `app/src/test/java/com/kmno/dropdate/data/mapper/ReleaseMapperTest.kt`:

```kotlin
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
        val entity = ReleaseEntity(
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
        val entity = ReleaseEntity(
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
```

- [ ] **Step 6: Run mapper tests**

```bash
gradlew.bat testDebugUnitTest --tests "com.kmno.dropdate.data.mapper.ReleaseMapperTest"
```

Expected: All 4 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/data/mapper/ReleaseMapper.kt
git add app/src/test/java/com/kmno/dropdate/data/mapper/ReleaseMapperTest.kt
git commit -m "feat: extract seriesId in mapper and add isTracked param to toDomain"
```

---

## Task 3: Update `Release` domain model + fix dependent tests

**Files:**
- Modify: `app/src/main/java/com/kmno/dropdate/domain/model/Release.kt`
- Modify: `app/src/test/java/com/kmno/dropdate/domain/usecase/GetWeekReleasesUseCaseTest.kt`

- [ ] **Step 1: Add `seriesId` and `isTracked` to `Release`**

Replace `app/src/main/java/com/kmno/dropdate/domain/model/Release.kt`:

```kotlin
package com.kmno.dropdate.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Release(
    val id: String,
    val seriesId: String,
    val isTracked: Boolean = false,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: ReleaseType,
    val status: ReleaseStatus,
    val airDate: LocalDate,
    val airTime: LocalTime?,
    val platform: String?,
    val episodeLabel: String?,
    val rating: Float?,
    val synopsis: String?,
    val genres: List<String> = emptyList(),
)
```

- [ ] **Step 2: Fix `GetWeekReleasesUseCaseTest` — add `seriesId` to `fakeRelease`**

Replace the full file `app/src/test/java/com/kmno/dropdate/domain/usecase/GetWeekReleasesUseCaseTest.kt`:

```kotlin
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
        seriesId = id,
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
```

- [ ] **Step 3: Build to verify no remaining compilation errors**

```bash
gradlew.bat testDebugUnitTest --tests "com.kmno.dropdate.domain.usecase.GetWeekReleasesUseCaseTest"
```

Expected: Both tests PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/domain/model/Release.kt
git add app/src/test/java/com/kmno/dropdate/domain/usecase/GetWeekReleasesUseCaseTest.kt
git commit -m "feat: add seriesId and isTracked to Release domain model"
```

---

## Task 4: Add tracking methods to `ReleaseDao`

**Files:**
- Modify: `app/src/main/java/com/kmno/dropdate/data/local/dao/ReleaseDao.kt`

> Note: Room DAOs require Android instrumented tests to test SQL. These methods are verified indirectly via repository unit tests in Task 5 and a build check here.

- [ ] **Step 1: Add tracking methods to `ReleaseDao`**

Replace the full file `app/src/main/java/com/kmno/dropdate/data/local/dao/ReleaseDao.kt`:

```kotlin
package com.kmno.dropdate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kmno.dropdate.data.local.entity.ReleaseEntity
import com.kmno.dropdate.data.local.entity.TrackedSeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {
    @Query(
        """
        SELECT * FROM releases
        WHERE airDate BETWEEN :from AND :to
        ORDER BY airDate ASC, rating DESC
    """,
    )
    fun observeByWeek(
        from: String,
        to: String,
    ): Flow<List<ReleaseEntity>>

    @Upsert
    suspend fun upsertAll(releases: List<ReleaseEntity>)

    @Query("DELETE FROM releases WHERE syncedAt < :threshold")
    suspend fun deleteStale(threshold: Long)

    @Query("DELETE FROM releases WHERE airDate < :before")
    suspend fun deleteOldReleases(before: String)

    @Query("SELECT * FROM releases WHERE title LIKE '%' || :query || '%'")
    fun searchReleasesTitle(query: String): Flow<List<ReleaseEntity>>

    // ── Tracking ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun trackSeries(entity: TrackedSeriesEntity)

    @Query("DELETE FROM tracked_series WHERE seriesId = :seriesId")
    suspend fun untrackSeries(seriesId: String)

    @Query("SELECT seriesId FROM tracked_series")
    fun observeTrackedSeriesIds(): Flow<List<String>>

    @Query(
        """
        SELECT * FROM releases
        WHERE seriesId IN (SELECT seriesId FROM tracked_series)
        ORDER BY airDate ASC
    """,
    )
    fun observeTracked(): Flow<List<ReleaseEntity>>
}
```

- [ ] **Step 2: Build to verify DAO compiles**

```bash
gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/data/local/dao/ReleaseDao.kt
git commit -m "feat: add tracking DAO methods to ReleaseDao"
```

---

## Task 5: Update `ReleaseRepository` interface and `ReleaseRepositoryImpl`

**Files:**
- Modify: `app/src/main/java/com/kmno/dropdate/domain/repository/ReleaseRepository.kt`
- Modify: `app/src/main/java/com/kmno/dropdate/data/repository/ReleaseRepositoryImpl.kt`

- [ ] **Step 1: Add methods to `ReleaseRepository` interface**

Replace `app/src/main/java/com/kmno/dropdate/domain/repository/ReleaseRepository.kt`:

```kotlin
package com.kmno.dropdate.domain.repository

import com.kmno.dropdate.domain.model.Release
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ReleaseRepository {
    fun getReleasesForWeek(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Flow<List<Release>>

    suspend fun syncReleases(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Result<Unit>

    suspend fun deleteOldReleases(before: LocalDate)

    fun searchReleasesTitle(query: String): Flow<List<Release>>

    suspend fun setTracking(seriesId: String, track: Boolean)

    fun getTrackedReleases(): Flow<List<Release>>
}
```

- [ ] **Step 2: Implement new methods in `ReleaseRepositoryImpl`; update `getReleasesForWeek`**

Replace `app/src/main/java/com/kmno/dropdate/data/repository/ReleaseRepositoryImpl.kt`:

```kotlin
package com.kmno.dropdate.data.repository

import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.local.entity.TrackedSeriesEntity
import com.kmno.dropdate.data.mapper.ReleaseMapper
import com.kmno.dropdate.data.remote.anilist.AniListApi
import com.kmno.dropdate.data.remote.anilist.aniListScheduleQuery
import com.kmno.dropdate.data.remote.tmdb.TmdbApi
import com.kmno.dropdate.data.remote.tvmaze.TvMazeApi
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
            combine(
                dao.observeByWeek(weekStart.toString(), weekEnd.toString()),
                dao.observeTrackedSeriesIds(),
            ) { entities, trackedIds ->
                val trackedSet = trackedIds.toSet()
                entities.map { mapper.toDomain(it, isTracked = it.seriesId in trackedSet) }
            }

        override suspend fun syncReleases(
            weekStart: LocalDate,
            weekEnd: LocalDate,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    coroutineScope {
                        val dayCount = (weekEnd.toEpochDay() - weekStart.toEpochDay() + 1).toInt()

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

                        val weekStartUnix =
                            weekStart.atStartOfDay().toEpochSecond(ZoneOffset.UTC).toInt()
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
            }

        override suspend fun deleteOldReleases(before: LocalDate) {
            dao.deleteOldReleases(before.toString())
        }

        override fun searchReleasesTitle(query: String): Flow<List<Release>> =
            dao.searchReleasesTitle(query).map { entities ->
                entities.map { mapper.toDomain(it) }
            }

        override suspend fun setTracking(seriesId: String, track: Boolean) =
            withContext(Dispatchers.IO) {
                if (track) dao.trackSeries(TrackedSeriesEntity(seriesId))
                else dao.untrackSeries(seriesId)
            }

        override fun getTrackedReleases(): Flow<List<Release>> =
            dao.observeTracked().map { entities ->
                entities.map { mapper.toDomain(it, isTracked = true) }
            }
    }
```

- [ ] **Step 3: Build to verify no compilation errors**

```bash
gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/domain/repository/ReleaseRepository.kt
git add app/src/main/java/com/kmno/dropdate/data/repository/ReleaseRepositoryImpl.kt
git commit -m "feat: implement setTracking and getTrackedReleases in repository"
```

---

## Task 6: Create `SetTrackingUseCase` with tests

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/domain/usecase/SetTrackingUseCase.kt`
- Create: `app/src/test/java/com/kmno/dropdate/domain/usecase/SetTrackingUseCaseTest.kt`

- [ ] **Step 1: Write the failing tests first**

Create `app/src/test/java/com/kmno/dropdate/domain/usecase/SetTrackingUseCaseTest.kt`:

```kotlin
package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDate

class SetTrackingUseCaseTest {
    private val repository: ReleaseRepository = mock()
    private val useCase = SetTrackingUseCase(repository)

    private fun fakeRelease(status: ReleaseStatus) = Release(
        id = "tvmaze_42_2025-08-01",
        seriesId = "tvmaze_42",
        title = "Test Show",
        posterUrl = null,
        backdropUrl = null,
        type = ReleaseType.SERIES,
        status = status,
        airDate = LocalDate.of(2025, 8, 1),
        airTime = null,
        platform = null,
        episodeLabel = "S01 · E05",
        rating = null,
        synopsis = null,
    )

    @Test
    fun `tracking upcoming release calls repository and returns success`() =
        runTest {
            val release = fakeRelease(ReleaseStatus.UPCOMING)
            whenever(repository.setTracking(release.seriesId, true)).thenReturn(Unit)

            val result = useCase(release, true)

            verify(repository).setTracking(release.seriesId, true)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `tracking released item fails without calling repository`() =
        runTest {
            val release = fakeRelease(ReleaseStatus.RELEASED)

            val result = useCase(release, true)

            verifyNoInteractions(repository)
            assertTrue(result.isFailure)
            assertEquals(
                "Only upcoming releases can be tracked",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `un-tracking a released item succeeds`() =
        runTest {
            val release = fakeRelease(ReleaseStatus.RELEASED)
            whenever(repository.setTracking(release.seriesId, false)).thenReturn(Unit)

            val result = useCase(release, false)

            verify(repository).setTracking(release.seriesId, false)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `un-tracking an upcoming item succeeds`() =
        runTest {
            val release = fakeRelease(ReleaseStatus.UPCOMING)
            whenever(repository.setTracking(release.seriesId, false)).thenReturn(Unit)

            val result = useCase(release, false)

            verify(repository).setTracking(release.seriesId, false)
            assertTrue(result.isSuccess)
        }
}
```

- [ ] **Step 2: Run to verify tests fail (class not found)**

```bash
gradlew.bat testDebugUnitTest --tests "com.kmno.dropdate.domain.usecase.SetTrackingUseCaseTest"
```

Expected: FAIL — `SetTrackingUseCase` does not exist yet.

- [ ] **Step 3: Create `SetTrackingUseCase`**

Create `app/src/main/java/com/kmno/dropdate/domain/usecase/SetTrackingUseCase.kt`:

```kotlin
package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.repository.ReleaseRepository
import javax.inject.Inject

class SetTrackingUseCase
    @Inject
    constructor(
        private val repository: ReleaseRepository,
    ) {
        suspend operator fun invoke(release: Release, track: Boolean): Result<Unit> {
            if (track && release.status != ReleaseStatus.UPCOMING) {
                return Result.failure(
                    IllegalStateException("Only upcoming releases can be tracked"),
                )
            }
            return runCatching { repository.setTracking(release.seriesId, track) }
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
gradlew.bat testDebugUnitTest --tests "com.kmno.dropdate.domain.usecase.SetTrackingUseCaseTest"
```

Expected: All 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/domain/usecase/SetTrackingUseCase.kt
git add app/src/test/java/com/kmno/dropdate/domain/usecase/SetTrackingUseCaseTest.kt
git commit -m "feat: add SetTrackingUseCase with business rule for UPCOMING-only tracking"
```

---

## Task 7: Create `GetTrackedReleasesUseCase` with tests

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/domain/usecase/GetTrackedReleasesUseCase.kt`
- Create: `app/src/test/java/com/kmno/dropdate/domain/usecase/GetTrackedReleasesUseCaseTest.kt`

- [ ] **Step 1: Write the failing tests first**

Create `app/src/test/java/com/kmno/dropdate/domain/usecase/GetTrackedReleasesUseCaseTest.kt`:

```kotlin
package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class GetTrackedReleasesUseCaseTest {
    private val repository: ReleaseRepository = mock()
    private val useCase = GetTrackedReleasesUseCase(repository)

    private fun fakeRelease(seriesId: String, isTracked: Boolean = true) = Release(
        id = "${seriesId}_2025-08-01",
        seriesId = seriesId,
        isTracked = isTracked,
        title = "Show $seriesId",
        posterUrl = null,
        backdropUrl = null,
        type = ReleaseType.SERIES,
        status = ReleaseStatus.UPCOMING,
        airDate = LocalDate.of(2025, 8, 1),
        airTime = null,
        platform = null,
        episodeLabel = null,
        rating = null,
        synopsis = null,
    )

    @Test
    fun `invoke returns flow of tracked releases from repository`() =
        runTest {
            val tracked = listOf(fakeRelease("tvmaze_1"), fakeRelease("anilist_42"))
            whenever(repository.getTrackedReleases()).thenReturn(flowOf(tracked))

            val result = useCase().first()

            assertEquals(tracked, result)
            assertTrue(result.all { it.isTracked })
        }

    @Test
    fun `invoke returns empty flow when nothing is tracked`() =
        runTest {
            whenever(repository.getTrackedReleases()).thenReturn(flowOf(emptyList()))

            val result = useCase().first()

            assertEquals(emptyList<Release>(), result)
        }
}
```

- [ ] **Step 2: Run to verify tests fail**

```bash
gradlew.bat testDebugUnitTest --tests "com.kmno.dropdate.domain.usecase.GetTrackedReleasesUseCaseTest"
```

Expected: FAIL — `GetTrackedReleasesUseCase` does not exist yet.

- [ ] **Step 3: Create `GetTrackedReleasesUseCase`**

Create `app/src/main/java/com/kmno/dropdate/domain/usecase/GetTrackedReleasesUseCase.kt`:

```kotlin
package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrackedReleasesUseCase
    @Inject
    constructor(
        private val repository: ReleaseRepository,
    ) {
        operator fun invoke(): Flow<List<Release>> = repository.getTrackedReleases()
    }
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
gradlew.bat testDebugUnitTest --tests "com.kmno.dropdate.domain.usecase.GetTrackedReleasesUseCaseTest"
```

Expected: Both tests PASS.

- [ ] **Step 5: Run all unit tests to verify nothing regressed**

```bash
gradlew.bat testDebugUnitTest
```

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/domain/usecase/GetTrackedReleasesUseCase.kt
git add app/src/test/java/com/kmno/dropdate/domain/usecase/GetTrackedReleasesUseCaseTest.kt
git commit -m "feat: add GetTrackedReleasesUseCase"
```

---

## Done

All backend layers are complete. The feature is ready for UI wiring (toggle in `ReleaseDetailSheet`, tracked list screen/tab). That is a separate task outside this plan's scope.
