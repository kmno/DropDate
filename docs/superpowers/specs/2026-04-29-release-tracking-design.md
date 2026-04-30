# Release Tracking — Design Spec
**Date:** 2026-04-29
**Status:** Approved

---

## Overview

Allow users to track upcoming releases (movies, TV series, anime) from the `ReleaseDetailSheet`. Tracking a series/anime tracks the whole series — not individual episodes. Tracked items remain visible in a dedicated tracked list even after they air. Tracked releases can be purged by the existing cleanup logic.

---

## Constraints & Rules

- Only `UPCOMING` releases may be tracked (enforced in use case layer).
- Tracking a series/anime tracks all its episodes — current and future synced ones.
- Movies are tracked individually (one row = one series).
- Tracked items are **not** protected from `deleteOldReleases` / `deleteStale` cleanup.
- Tracking state survives API re-syncs — new episodes of a tracked series automatically appear as tracked.
- UI entry point: `ReleaseDetailSheet` only.

---

## Architecture

Clean Architecture: Room → DAO → Repository → Use Cases → ViewModel.
Hilt for DI throughout. Kotlin Flows + `combine` for reactive tracking state.

---

## Data Layer

### `ReleaseEntity` — add one field

```kotlin
val seriesId: String,
```

Migration (version bump, e.g. 1 → 2):
```sql
ALTER TABLE releases ADD COLUMN seriesId TEXT NOT NULL DEFAULT ''
```

`seriesId` values by source:
| Source  | `id` pattern                          | `seriesId`             |
|---------|---------------------------------------|------------------------|
| TMDB    | `tmdb_m_{movieId}`                    | `tmdb_m_{movieId}`     |
| TvMaze  | `tvmaze_{showId}_{date}`              | `tvmaze_{showId}`      |
| AniList | `anilist_{mediaId}_{date}_ep{ep}`     | `anilist_{mediaId}`    |

This groups all episodes of the same series under one stable key. The existing `@Upsert` is unchanged.

### New `TrackedSeriesEntity`

```kotlin
@Entity(tableName = "tracked_series")
data class TrackedSeriesEntity(
    @PrimaryKey val seriesId: String,
)
```

Inserting a `seriesId` means the user is tracking that series. Deleting it un-tracks it. New episodes synced from the API automatically appear as tracked because queries filter by `seriesId`, not episode `id`.

### `ReleaseDao` — additions

```kotlin
// Tracking mutations
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun trackSeries(entity: TrackedSeriesEntity)

@Query("DELETE FROM tracked_series WHERE seriesId = :seriesId")
suspend fun untrackSeries(seriesId: String)

// Reactive reads
@Query("SELECT seriesId FROM tracked_series")
fun observeTrackedSeriesIds(): Flow<List<String>>

@Query("""
    SELECT * FROM releases
    WHERE seriesId IN (SELECT seriesId FROM tracked_series)
    ORDER BY airDate ASC
""")
fun observeTracked(): Flow<List<ReleaseEntity>>
```

### `AppDatabase`

- Add `TrackedSeriesEntity` to `entities`
- Bump `version` by 1
- Add `Migration(oldVersion, newVersion)` with the `ALTER TABLE` SQL above

---

## Domain Layer

### `Release` — add two fields

```kotlin
val seriesId: String,
val isTracked: Boolean = false,
```

### `ReleaseMapper` — two updates

1. Extract and set `seriesId` in each source mapper (`fromTmdb`, `fromTvMaze`, `fromAniList`).
2. `toDomain` accepts an `isTracked` parameter:

```kotlin
fun toDomain(entity: ReleaseEntity, isTracked: Boolean = false): Release
```

### `ReleaseRepository` — add two methods

```kotlin
suspend fun setTracking(seriesId: String, track: Boolean)
fun getTrackedReleases(): Flow<List<Release>>
```

### `SetTrackingUseCase`

Enforces the business rule that only `UPCOMING` releases can be tracked:

```kotlin
class SetTrackingUseCase @Inject constructor(private val repository: ReleaseRepository) {
    suspend operator fun invoke(release: Release, track: Boolean): Result<Unit> {
        if (track && release.status != ReleaseStatus.UPCOMING) {
            return Result.failure(IllegalStateException("Only upcoming releases can be tracked"))
        }
        return runCatching { repository.setTracking(release.seriesId, track) }
    }
}
```

### `GetTrackedReleasesUseCase`

```kotlin
class GetTrackedReleasesUseCase @Inject constructor(private val repository: ReleaseRepository) {
    operator fun invoke(): Flow<List<Release>> = repository.getTrackedReleases()
}
```

---

## Repository Implementation

### `getReleasesForWeek` — derive `isTracked` via `combine`

```kotlin
override fun getReleasesForWeek(weekStart: LocalDate, weekEnd: LocalDate): Flow<List<Release>> =
    combine(
        dao.observeByWeek(weekStart.toString(), weekEnd.toString()),
        dao.observeTrackedSeriesIds()
    ) { entities, trackedIds ->
        val trackedSet = trackedIds.toSet()
        entities.map { mapper.toDomain(it, isTracked = it.seriesId in trackedSet) }
    }
```

### `setTracking`

```kotlin
override suspend fun setTracking(seriesId: String, track: Boolean) =
    withContext(Dispatchers.IO) {
        if (track) dao.trackSeries(TrackedSeriesEntity(seriesId))
        else dao.untrackSeries(seriesId)
    }
```

### `getTrackedReleases`

```kotlin
override fun getTrackedReleases(): Flow<List<Release>> =
    combine(
        dao.observeTracked(),
        dao.observeTrackedSeriesIds()
    ) { entities, trackedIds ->
        val trackedSet = trackedIds.toSet()
        entities.map { mapper.toDomain(it, isTracked = it.seriesId in trackedSet) }
    }
```

---

## Dependency Injection

No explicit `@Provides` needed. `SetTrackingUseCase` and `GetTrackedReleasesUseCase` use `@Inject constructor` — Hilt binds them automatically. `AppDatabase` must register `TrackedSeriesEntity` in its `entities` list.

---

## Files Changed

| File | Change |
|------|--------|
| `data/local/entity/ReleaseEntity.kt` | Add `seriesId: String` |
| `data/local/entity/TrackedSeriesEntity.kt` | **New** |
| `data/local/dao/ReleaseDao.kt` | Add 4 new DAO methods |
| `data/local/AppDatabase.kt` | Register new entity, bump version, add migration |
| `data/mapper/ReleaseMapper.kt` | Extract `seriesId`; update `toDomain` signature |
| `data/repository/ReleaseRepositoryImpl.kt` | Implement `setTracking`, `getTrackedReleases`; update `getReleasesForWeek` |
| `domain/model/Release.kt` | Add `seriesId`, `isTracked` |
| `domain/repository/ReleaseRepository.kt` | Add `setTracking`, `getTrackedReleases` |
| `domain/usecase/SetTrackingUseCase.kt` | **New** |
| `domain/usecase/GetTrackedReleasesUseCase.kt` | **New** |