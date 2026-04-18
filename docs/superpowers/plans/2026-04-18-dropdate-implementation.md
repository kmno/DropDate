# DropDate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold the full Clean Architecture structure for DropDate — a personal release tracker for movies, series, and anime — with one working screen, offline-first Room cache, four Retrofit API clients, Hilt DI, Coil image loading, and a high-contrast animated Compose UI.

**Architecture:** Layer-first (core/data/domain/di) with feature-grouped presentation (`presentation/schedule/`). Room is the single source of truth; network only writes to Room, UI only reads from Room. The ViewModel reacts to state changes via `flatMapLatest` so filter and week changes automatically re-subscribe the Room flow.

**Tech Stack:** Kotlin 2.3.20, Jetpack Compose BOM 2026.03.01, Hilt 2.59.2, Room 2.8.4, Retrofit 3.0.0, OkHttp 5.3.2, Coil 3.1.0, Navigation Compose 2.9.7, kotlinx-serialization-json 1.11.0, kotlinx-coroutines 1.10.2

---

## File Map

```
app/src/main/java/com/kmno/dropdate/
├── core/
│   ├── AppError.kt
│   └── ext/FlowExt.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/ReleaseDao.kt
│   │   └── entity/ReleaseEntity.kt
│   ├── remote/
│   │   ├── tmdb/TmdbApi.kt + TmdbDto.kt
│   │   ├── tvmaze/TvMazeApi.kt + TvMazeDto.kt
│   │   ├── jikan/JikanApi.kt + JikanDto.kt
│   │   └── animeschedule/AnimeScheduleApi.kt + AnimeScheduleDto.kt
│   ├── mapper/ReleaseMapper.kt
│   └── repository/ReleaseRepositoryImpl.kt
├── domain/
│   ├── model/Release.kt + ReleaseType.kt + ReleaseStatus.kt
│   ├── repository/ReleaseRepository.kt
│   └── usecase/GetWeekReleasesUseCase.kt + SyncReleasesUseCase.kt
├── presentation/
│   ├── navigation/Screen.kt + NavGraph.kt
│   └── schedule/
│       ├── ScheduleUiState.kt
│       ├── ScheduleViewModel.kt
│       ├── ScheduleScreen.kt
│       └── components/
│           ├── WeekScroller.kt
│           ├── ContentTypeChips.kt
│           ├── ReleaseSection.kt
│           ├── ReleaseCard.kt
│           ├── ReleaseDetailSheet.kt
│           ├── WatchBadge.kt
│           └── CountdownText.kt
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── DropDateApp.kt
└── MainActivity.kt

app/src/main/res/values/themes.xml  (modify — add splash theme)
app/src/main/AndroidManifest.xml    (modify — add App class, INTERNET permission, splash)
local.properties                    (modify — add TMDB_API_KEY)
app/build.gradle.kts                (modify — add buildConfigField)
gradle/libs.versions.toml           (modify — add coil)

app/src/test/java/com/kmno/dropdate/
├── domain/usecase/GetWeekReleasesUseCaseTest.kt
├── domain/usecase/SyncReleasesUseCaseTest.kt
├── data/mapper/ReleaseMapperTest.kt
└── presentation/schedule/ScheduleViewModelTest.kt
```

---

### Task 1: Add missing dependencies (Coil + buildConfigField)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `local.properties`

- [ ] **Step 1: Add Coil entries to version catalog**

Open `gradle/libs.versions.toml`. Add after the `splashscreen` version line:

```toml
coil = "3.1.0"
```

Add after the `androidx-core-splashscreen` library line:

```toml
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
```

- [ ] **Step 2: Add Coil deps + buildConfigField to app/build.gradle.kts**

In the `android {}` block, add after `buildFeatures { compose = true }`:

```kotlin
buildConfigField("String", "TMDB_API_KEY", "\"${project.findProperty("TMDB_API_KEY") ?: ""}\"")
buildFeatures {
    compose = true
    buildConfig = true
}
```

Replace the existing `buildFeatures` block with the above (it merges both).

In `dependencies {}`, add after `implementation(libs.androidx.core.splashscreen)`:

```kotlin
implementation(libs.coil.compose)
implementation(libs.coil.network.okhttp)
```

- [ ] **Step 3: Add placeholder TMDB key to local.properties**

Append to `local.properties`:

```
TMDB_API_KEY=YOUR_KEY_HERE
```

Replace `YOUR_KEY_HERE` with your actual free TMDB API key from https://www.themoviedb.org/settings/api

- [ ] **Step 4: Sync and verify build**

Run: `gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL — no missing dependency errors.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts local.properties
git commit -m "build: add Coil deps and TMDB buildConfigField"
```

---

### Task 2: Manifest + Application class bootstrap

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/kmno/dropdate/DropDateApp.kt`
- Modify: `app/src/main/java/com/kmno/dropdate/MainActivity.kt`

- [ ] **Step 1: Update AndroidManifest.xml**

Replace the entire file content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".DropDateApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.DropDate">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.DropDate">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 2: Create DropDateApp.kt**

```kotlin
package com.kmno.dropdate

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class DropDateApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
}
```

- [ ] **Step 3: Update MainActivity.kt**

```kotlin
package com.kmno.dropdate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kmno.dropdate.presentation.navigation.DropDateNavGraph
import com.kmno.dropdate.ui.theme.DropDateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DropDateTheme {
                DropDateNavGraph()
            }
        }
    }
}
```

- [ ] **Step 4: Verify build compiles**

Run: `gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL. Hilt will warn about missing `@HiltAndroidApp` components — this is expected until DI modules are in place in Task 6.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/kmno/dropdate/DropDateApp.kt app/src/main/java/com/kmno/dropdate/MainActivity.kt
git commit -m "feat: bootstrap Application class with Hilt and Coil"
```

---

### Task 3: Core layer — AppError + FlowExt

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/core/AppError.kt`
- Create: `app/src/main/java/com/kmno/dropdate/core/ext/FlowExt.kt`

- [ ] **Step 1: Create AppError.kt**

```kotlin
package com.kmno.dropdate.core

sealed interface AppError {
    data class Network(val message: String) : AppError
    data class Database(val message: String) : AppError
    data object Unknown : AppError
}
```

- [ ] **Step 2: Create FlowExt.kt**

```kotlin
package com.kmno.dropdate.core.ext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

sealed interface FlowResult<out T> {
    data class Success<T>(val data: T) : FlowResult<T>
    data class Error(val throwable: Throwable) : FlowResult<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<FlowResult<T>> =
    map<T, FlowResult<T>> { FlowResult.Success(it) }
        .catch { emit(FlowResult.Error(it)) }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/core/
git commit -m "feat: add core AppError and FlowExt"
```

---

### Task 4: Domain layer — models, repository interface, use cases

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/domain/model/ReleaseType.kt`
- Create: `app/src/main/java/com/kmno/dropdate/domain/model/ReleaseStatus.kt`
- Create: `app/src/main/java/com/kmno/dropdate/domain/model/Release.kt`
- Create: `app/src/main/java/com/kmno/dropdate/domain/repository/ReleaseRepository.kt`
- Create: `app/src/main/java/com/kmno/dropdate/domain/usecase/GetWeekReleasesUseCase.kt`
- Create: `app/src/main/java/com/kmno/dropdate/domain/usecase/SyncReleasesUseCase.kt`
- Test: `app/src/test/java/com/kmno/dropdate/domain/usecase/GetWeekReleasesUseCaseTest.kt`
- Test: `app/src/test/java/com/kmno/dropdate/domain/usecase/SyncReleasesUseCaseTest.kt`

- [ ] **Step 1: Create ReleaseType.kt**

```kotlin
package com.kmno.dropdate.domain.model

enum class ReleaseType { MOVIE, SERIES, ANIME }
```

- [ ] **Step 2: Create ReleaseStatus.kt**

```kotlin
package com.kmno.dropdate.domain.model

enum class ReleaseStatus { UPCOMING, RELEASED }
```

- [ ] **Step 3: Create Release.kt**

```kotlin
package com.kmno.dropdate.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Release(
    val id: String,
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
)
```

- [ ] **Step 4: Create ReleaseRepository.kt**

```kotlin
package com.kmno.dropdate.domain.repository

import com.kmno.dropdate.domain.model.Release
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ReleaseRepository {
    fun getReleasesForWeek(weekStart: LocalDate, weekEnd: LocalDate): Flow<List<Release>>
    suspend fun syncReleases(weekStart: LocalDate, weekEnd: LocalDate): Result<Unit>
}
```

- [ ] **Step 5: Create GetWeekReleasesUseCase.kt**

```kotlin
package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetWeekReleasesUseCase @Inject constructor(
    private val repository: ReleaseRepository
) {
    operator fun invoke(weekStart: LocalDate, weekEnd: LocalDate): Flow<List<Release>> =
        repository.getReleasesForWeek(weekStart, weekEnd)
}
```

- [ ] **Step 6: Create SyncReleasesUseCase.kt**

```kotlin
package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.repository.ReleaseRepository
import java.time.LocalDate
import javax.inject.Inject

class SyncReleasesUseCase @Inject constructor(
    private val repository: ReleaseRepository
) {
    suspend operator fun invoke(weekStart: LocalDate, weekEnd: LocalDate): Result<Unit> =
        repository.syncReleases(weekStart, weekEnd)
}
```

- [ ] **Step 7: Write failing use case tests**

Create `app/src/test/java/com/kmno/dropdate/domain/usecase/GetWeekReleasesUseCaseTest.kt`:

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

    private fun fakeRelease(id: String, date: LocalDate) = Release(
        id = id, title = "Title $id", posterUrl = null, backdropUrl = null,
        type = ReleaseType.MOVIE, status = ReleaseStatus.UPCOMING,
        airDate = date, airTime = null, platform = null,
        episodeLabel = null, rating = null, synopsis = null,
    )

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val releases = listOf(fakeRelease("1", monday), fakeRelease("2", sunday))
        whenever(repository.getReleasesForWeek(monday, sunday)).thenReturn(flowOf(releases))

        val result = useCase(monday, sunday).first()

        assertEquals(releases, result)
    }

    @Test
    fun `invoke returns empty list when repository emits empty`() = runTest {
        whenever(repository.getReleasesForWeek(monday, sunday)).thenReturn(flowOf(emptyList()))

        val result = useCase(monday, sunday).first()

        assertEquals(emptyList<Release>(), result)
    }
}
```

Create `app/src/test/java/com/kmno/dropdate/domain/usecase/SyncReleasesUseCaseTest.kt`:

```kotlin
package com.kmno.dropdate.domain.usecase

import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.test.runTest
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
    fun `invoke delegates to repository and returns success`() = runTest {
        whenever(repository.syncReleases(monday, sunday)).thenReturn(Result.success(Unit))

        val result = useCase(monday, sunday)

        verify(repository).syncReleases(monday, sunday)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        whenever(repository.syncReleases(monday, sunday))
            .thenReturn(Result.failure(RuntimeException("network error")))

        val result = useCase(monday, sunday)

        assertTrue(result.isFailure)
        assertEquals("network error", result.exceptionOrNull()?.message)
    }
}
```

- [ ] **Step 8: Add test dependencies to app/build.gradle.kts**

In `dependencies {}`, add:

```kotlin
testImplementation(libs.mockito.kotlin)
testImplementation(libs.kotlinx.coroutines.test)
```

Add to `libs.versions.toml` under `[versions]`:
```toml
mockitoKotlin = "5.4.0"
```

Add under `[libraries]`:
```toml
mockito-kotlin = { group = "org.mockito.kotlin", name = "mockito-kotlin", version.ref = "mockitoKotlin" }
```

- [ ] **Step 9: Run tests — expect FAIL (repository not implemented yet)**

Run: `gradlew.bat testDebugUnitTest --tests "*.domain.usecase.*"`
Expected: Tests compile and run. `GetWeekReleasesUseCaseTest` passes (pure delegation). `SyncReleasesUseCaseTest` passes (pure delegation via mock).

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/domain/ app/src/test/java/com/kmno/dropdate/domain/ gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat: add domain layer — models, repository interface, use cases + tests"
```

---

### Task 5: Data layer — Room entity, DAO, and database

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/data/local/entity/ReleaseEntity.kt`
- Create: `app/src/main/java/com/kmno/dropdate/data/local/dao/ReleaseDao.kt`
- Create: `app/src/main/java/com/kmno/dropdate/data/local/AppDatabase.kt`

- [ ] **Step 1: Create ReleaseEntity.kt**

```kotlin
package com.kmno.dropdate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "releases")
data class ReleaseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: String,
    val status: String,
    val airDate: String,
    val airTime: String?,
    val platform: String?,
    val episodeLabel: String?,
    val rating: Float?,
    val synopsis: String?,
    val syncedAt: Long,
)
```

- [ ] **Step 2: Create ReleaseDao.kt**

```kotlin
package com.kmno.dropdate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kmno.dropdate.data.local.entity.ReleaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {

    @Query("""
        SELECT * FROM releases
        WHERE airDate BETWEEN :from AND :to
        ORDER BY airDate ASC, rating DESC
    """)
    fun observeByWeek(from: String, to: String): Flow<List<ReleaseEntity>>

    @Upsert
    suspend fun upsertAll(releases: List<ReleaseEntity>)

    @Query("DELETE FROM releases WHERE syncedAt < :threshold")
    suspend fun deleteStale(threshold: Long)
}
```

- [ ] **Step 3: Create AppDatabase.kt**

```kotlin
package com.kmno.dropdate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.local.entity.ReleaseEntity

@Database(entities = [ReleaseEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun releaseDao(): ReleaseDao
}
```

- [ ] **Step 4: Verify compilation**

Run: `gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL — Room annotation processor generates the DB implementation.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/data/local/
git commit -m "feat: add Room entity, DAO, and database"
```

---

### Task 6: Data layer — DTOs for all four APIs

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/data/remote/tmdb/TmdbDto.kt`
- Create: `app/src/main/java/com/kmno/dropdate/data/remote/tvmaze/TvMazeDto.kt`
- Create: `app/src/main/java/com/kmno/dropdate/data/remote/jikan/JikanDto.kt`
- Create: `app/src/main/java/com/kmno/dropdate/data/remote/animeschedule/AnimeScheduleDto.kt`

- [ ] **Step 1: Create TmdbDto.kt**

```kotlin
package com.kmno.dropdate.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovieListDto(
    val results: List<TmdbMovieDto> = emptyList(),
)

@Serializable
data class TmdbMovieDto(
    val id: Int,
    val title: String,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("release_date") val releaseDate: String?,
    @SerialName("vote_average") val voteAverage: Float?,
    val overview: String?,
)
```

- [ ] **Step 2: Create TvMazeDto.kt**

```kotlin
package com.kmno.dropdate.data.remote.tvmaze

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvMazeScheduleEntryDto(
    val id: Int,
    val name: String?,
    val airdate: String?,
    val airtime: String?,
    val runtime: Int?,
    val show: TvMazeShowDto?,
    val season: Int?,
    val number: Int?,
)

@Serializable
data class TvMazeShowDto(
    val id: Int,
    val name: String,
    val image: TvMazeImageDto?,
    val rating: TvMazeRatingDto?,
    val summary: String?,
    @SerialName("webChannel") val webChannel: TvMazeNetworkDto?,
    val network: TvMazeNetworkDto?,
)

@Serializable
data class TvMazeImageDto(
    val medium: String?,
    val original: String?,
)

@Serializable
data class TvMazeRatingDto(
    val average: Float?,
)

@Serializable
data class TvMazeNetworkDto(
    val name: String?,
)
```

- [ ] **Step 3: Create JikanDto.kt**

```kotlin
package com.kmno.dropdate.data.remote.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanSeasonDto(
    val data: List<JikanAnimeDto> = emptyList(),
)

@Serializable
data class JikanAnimeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String,
    val images: JikanImagesDto?,
    val score: Float?,
    val synopsis: String?,
    val status: String?,
    @SerialName("aired") val aired: JikanAiredDto?,
    @SerialName("broadcast") val broadcast: JikanBroadcastDto?,
    val episodes: Int?,
)

@Serializable
data class JikanImagesDto(
    val jpg: JikanImageDto?,
)

@Serializable
data class JikanImageDto(
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("large_image_url") val largeImageUrl: String?,
)

@Serializable
data class JikanAiredDto(
    val from: String?,
)

@Serializable
data class JikanBroadcastDto(
    val day: String?,
    val time: String?,
    val timezone: String?,
)
```

- [ ] **Step 4: Create AnimeScheduleDto.kt**

```kotlin
package com.kmno.dropdate.data.remote.animeschedule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeScheduleEntryDto(
    val route: String,
    val title: String,
    @SerialName("imageVersionRoute") val imageRoute: String?,
    @SerialName("episodeDate") val episodeDate: String?,
    @SerialName("episodeTime") val episodeTime: String?,
    val streams: List<AnimeScheduleStreamDto> = emptyList(),
    val score: Float?,
    val description: String?,
)

@Serializable
data class AnimeScheduleStreamDto(
    val name: String?,
)
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/data/remote/
git commit -m "feat: add Retrofit DTOs for TMDB, TVMaze, Jikan, AnimeSchedule"
```

---

### Task 7: Data layer — Retrofit API interfaces

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/data/remote/tmdb/TmdbApi.kt`
- Create: `app/src/main/java/com/kmno/dropdate/data/remote/tvmaze/TvMazeApi.kt`
- Create: `app/src/main/java/com/kmno/dropdate/data/remote/jikan/JikanApi.kt`
- Create: `app/src/main/java/com/kmno/dropdate/data/remote/animeschedule/AnimeScheduleApi.kt`

- [ ] **Step 1: Create TmdbApi.kt**

```kotlin
package com.kmno.dropdate.data.remote.tmdb

import com.kmno.dropdate.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("page") page: Int = 1,
    ): TmdbMovieListDto

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
    ): TmdbMovieListDto
}
```

- [ ] **Step 2: Create TvMazeApi.kt**

```kotlin
package com.kmno.dropdate.data.remote.tvmaze

import retrofit2.http.GET
import retrofit2.http.Query

interface TvMazeApi {
    @GET("schedule/web")
    suspend fun getStreamingSchedule(
        @Query("date") date: String,
    ): List<TvMazeScheduleEntryDto>

    @GET("schedule")
    suspend fun getBroadcastSchedule(
        @Query("date") date: String,
    ): List<TvMazeScheduleEntryDto>
}
```

- [ ] **Step 3: Create JikanApi.kt**

```kotlin
package com.kmno.dropdate.data.remote.jikan

import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApi {
    @GET("seasons/now")
    suspend fun getCurrentlyAiringAnime(
        @Query("page") page: Int = 1,
    ): JikanSeasonDto

    @GET("seasons/upcoming")
    suspend fun getUpcomingAnime(): JikanSeasonDto
}
```

- [ ] **Step 4: Create AnimeScheduleApi.kt**

```kotlin
package com.kmno.dropdate.data.remote.animeschedule

import retrofit2.http.GET
import retrofit2.http.Query

interface AnimeScheduleApi {
    @GET("timetables")
    suspend fun getTimetable(
        @Query("tz") timezone: String = "UTC",
    ): List<AnimeScheduleEntryDto>
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/data/remote/
git commit -m "feat: add Retrofit API interfaces for all four sources"
```

---

### Task 8: Data layer — ReleaseMapper

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/data/mapper/ReleaseMapper.kt`
- Test: `app/src/test/java/com/kmno/dropdate/data/mapper/ReleaseMapperTest.kt`

- [ ] **Step 1: Write failing mapper tests**

Create `app/src/test/java/com/kmno/dropdate/data/mapper/ReleaseMapperTest.kt`:

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

    // --- TMDB ---

    @Test
    fun `fromTmdb maps movie with future date to UPCOMING`() {
        val futureDate = LocalDate.now().plusDays(5).toString()
        val dto = TmdbMovieListDto(results = listOf(
            TmdbMovieDto(
                id = 1, title = "Dune 3",
                posterPath = "/poster.jpg", backdropPath = "/backdrop.jpg",
                releaseDate = futureDate, voteAverage = 8.5f, overview = "Epic."
            )
        ))

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
        val dto = TmdbMovieListDto(results = listOf(
            TmdbMovieDto(
                id = 2, title = "Old Movie",
                posterPath = null, backdropPath = null,
                releaseDate = pastDate, voteAverage = null, overview = null
            )
        ))

        val entities = mapper.fromTmdb(dto, TmdbMovieListDto())

        assertEquals("RELEASED", entities.first().status)
    }

    @Test
    fun `fromTmdb skips movies with null or blank release date`() {
        val dto = TmdbMovieListDto(results = listOf(
            TmdbMovieDto(id = 3, title = "No Date", posterPath = null,
                backdropPath = null, releaseDate = null, voteAverage = null, overview = null)
        ))

        val entities = mapper.fromTmdb(dto, TmdbMovieListDto())

        assertEquals(0, entities.size)
    }

    // --- Entity → Domain ---

    @Test
    fun `toDomain maps entity to Release correctly`() {
        val entity = ReleaseEntity(
            id = "tmdb_1", title = "Dune 3",
            posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/original/backdrop.jpg",
            type = "MOVIE", status = "UPCOMING",
            airDate = "2025-08-01", airTime = null,
            platform = null, episodeLabel = null,
            rating = 8.5f, synopsis = "Epic.",
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
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `gradlew.bat testDebugUnitTest --tests "*.data.mapper.*"`
Expected: FAIL — `ReleaseMapper` does not exist yet.

- [ ] **Step 3: Create ReleaseMapper.kt**

```kotlin
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
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `gradlew.bat testDebugUnitTest --tests "*.data.mapper.*"`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/data/mapper/ app/src/test/java/com/kmno/dropdate/data/mapper/
git commit -m "feat: add ReleaseMapper with full mapping from all 4 sources + tests"
```

---

### Task 9: Data layer — ReleaseRepositoryImpl

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/data/repository/ReleaseRepositoryImpl.kt`

- [ ] **Step 1: Create ReleaseRepositoryImpl.kt**

```kotlin
package com.kmno.dropdate.data.repository

import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.mapper.ReleaseMapper
import com.kmno.dropdate.data.remote.animeschedule.AnimeScheduleApi
import com.kmno.dropdate.data.remote.jikan.JikanApi
import com.kmno.dropdate.data.remote.tmdb.TmdbApi
import com.kmno.dropdate.data.remote.tvmaze.TvMazeApi
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.repository.ReleaseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class ReleaseRepositoryImpl @Inject constructor(
    private val dao: ReleaseDao,
    private val tmdbApi: TmdbApi,
    private val tvMazeApi: TvMazeApi,
    private val jikanApi: JikanApi,
    private val animeScheduleApi: AnimeScheduleApi,
    private val mapper: ReleaseMapper,
) : ReleaseRepository {

    override fun getReleasesForWeek(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Flow<List<Release>> =
        dao.observeByWeek(weekStart.toString(), weekEnd.toString())
            .map { entities -> entities.map(mapper::toDomain) }

    override suspend fun syncReleases(
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Result<Unit> = runCatching {
        coroutineScope {
            val upcoming      = async { tmdbApi.getUpcomingMovies() }
            val popular       = async { tmdbApi.getPopularMovies() }
            val streaming     = async { tvMazeApi.getStreamingSchedule(weekStart.toString()) }
            val broadcast     = async { tvMazeApi.getBroadcastSchedule(weekStart.toString()) }
            val airingAnime   = async { jikanApi.getCurrentlyAiringAnime() }
            val upcomingAnime = async { jikanApi.getUpcomingAnime() }
            val timetable     = async { animeScheduleApi.getTimetable() }

            val entities = buildList {
                addAll(mapper.fromTmdb(upcoming.await(), popular.await()))
                addAll(mapper.fromTvMaze(streaming.await(), broadcast.await()))
                addAll(mapper.fromJikan(airingAnime.await(), upcomingAnime.await(), timetable.await()))
            }

            dao.upsertAll(entities)
            dao.deleteStale(System.currentTimeMillis() - 7.days.inWholeMilliseconds)
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/data/repository/
git commit -m "feat: add ReleaseRepositoryImpl — offline-first Room + concurrent 4-API sync"
```

---

### Task 10: DI modules

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/di/AppModule.kt`
- Create: `app/src/main/java/com/kmno/dropdate/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/kmno/dropdate/di/NetworkModule.kt`
- Create: `app/src/main/java/com/kmno/dropdate/di/RepositoryModule.kt`

- [ ] **Step 1: Create AppModule.kt**

```kotlin
package com.kmno.dropdate.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

- [ ] **Step 2: Create DatabaseModule.kt**

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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "dropdate.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides @Singleton
    fun provideReleaseDao(db: AppDatabase): ReleaseDao = db.releaseDao()
}
```

- [ ] **Step 3: Create NetworkModule.kt**

```kotlin
package com.kmno.dropdate.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kmno.dropdate.data.remote.animeschedule.AnimeScheduleApi
import com.kmno.dropdate.data.remote.jikan.JikanApi
import com.kmno.dropdate.data.remote.tmdb.TmdbApi
import com.kmno.dropdate.data.remote.tvmaze.TvMazeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.kmno.dropdate.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                        else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideTmdbApi(client: OkHttpClient, json: Json): TmdbApi =
        buildRetrofit("https://api.themoviedb.org/3/", client, json).create(TmdbApi::class.java)

    @Provides @Singleton
    fun provideTvMazeApi(client: OkHttpClient, json: Json): TvMazeApi =
        buildRetrofit("https://api.tvmaze.com/", client, json).create(TvMazeApi::class.java)

    @Provides @Singleton
    fun provideJikanApi(client: OkHttpClient, json: Json): JikanApi =
        buildRetrofit("https://api.jikan.moe/v4/", client, json).create(JikanApi::class.java)

    @Provides @Singleton
    fun provideAnimeScheduleApi(client: OkHttpClient, json: Json): AnimeScheduleApi =
        buildRetrofit("https://animeschedule.net/api/v3/", client, json).create(AnimeScheduleApi::class.java)
}
```

- [ ] **Step 4: Create RepositoryModule.kt**

```kotlin
package com.kmno.dropdate.di

import com.kmno.dropdate.data.repository.ReleaseRepositoryImpl
import com.kmno.dropdate.domain.repository.ReleaseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindReleaseRepository(impl: ReleaseRepositoryImpl): ReleaseRepository
}
```

- [ ] **Step 5: Build and verify Hilt graph compiles**

Run: `gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL — Hilt generates the component with all bindings resolved, no missing binding errors.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/di/
git commit -m "feat: add Hilt DI modules — App, Database, Network, Repository"
```

---

### Task 11: Theme — colors, typography, dark theme

**Files:**
- Modify: `app/src/main/java/com/kmno/dropdate/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/kmno/dropdate/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/kmno/dropdate/ui/theme/Theme.kt`

- [ ] **Step 1: Replace Color.kt**

```kotlin
package com.kmno.dropdate.ui.theme

import androidx.compose.ui.graphics.Color

// Background layers
val Background  = Color(0xFF080810)
val Surface     = Color(0xFF12121E)
val SurfaceAlt  = Color(0xFF1A1A2E)

// Content-type accents
val MovieAmber  = Color(0xFFF5A623)
val SeriesBlue  = Color(0xFF4A9EFF)
val AnimePurple = Color(0xFFC084FC)

// Status
val ReleasedGreen = Color(0xFF22C55E)

// Text
val TextPrimary   = Color(0xFFF1F1F5)
val TextSecondary = Color(0xFF6B6B85)

// Legacy (kept so Theme.kt compiles until replaced below)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

- [ ] **Step 2: Replace Type.kt**

```kotlin
package com.kmno.dropdate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        color = TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = TextSecondary,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary,
    ),
)
```

- [ ] **Step 3: Replace Theme.kt**

```kotlin
package com.kmno.dropdate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DropDateColorScheme = darkColorScheme(
    primary         = SeriesBlue,
    secondary       = MovieAmber,
    tertiary        = AnimePurple,
    background      = Background,
    surface         = Surface,
    onPrimary       = TextPrimary,
    onSecondary     = TextPrimary,
    onBackground    = TextPrimary,
    onSurface       = TextPrimary,
    surfaceVariant  = SurfaceAlt,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun DropDateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DropDateColorScheme,
        typography  = Typography,
        content     = content,
    )
}
```

- [ ] **Step 4: Build**

Run: `gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/ui/theme/
git commit -m "feat: apply DropDate dark high-contrast theme"
```

---

### Task 12: Navigation scaffold

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/presentation/navigation/Screen.kt`
- Create: `app/src/main/java/com/kmno/dropdate/presentation/navigation/NavGraph.kt`

- [ ] **Step 1: Create Screen.kt**

```kotlin
package com.kmno.dropdate.presentation.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object Schedule : Screen()
}
```

- [ ] **Step 2: Create NavGraph.kt** (with placeholder screen for now)

```kotlin
package com.kmno.dropdate.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kmno.dropdate.presentation.schedule.ScheduleScreen

@Composable
fun DropDateNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Schedule) {
        composable<Screen.Schedule> {
            ScheduleScreen()
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/presentation/navigation/
git commit -m "feat: add navigation scaffold with Schedule destination"
```

---

### Task 13: Presentation — UiState + ViewModel

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/ScheduleUiState.kt`
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/ScheduleViewModel.kt`
- Test: `app/src/test/java/com/kmno/dropdate/presentation/schedule/ScheduleViewModelTest.kt`

- [ ] **Step 1: Create ScheduleUiState.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule

import com.kmno.dropdate.domain.model.Release
import java.time.DayOfWeek
import java.time.LocalDate

enum class ContentFilter { ALL, MOVIES, SERIES, ANIME }

data class ScheduleUiState(
    val selectedWeekStart: LocalDate  = LocalDate.now().with(DayOfWeek.MONDAY),
    val selectedDay: LocalDate        = LocalDate.now(),
    val activeFilter: ContentFilter   = ContentFilter.ALL,
    val releases: Map<LocalDate, List<Release>> = emptyMap(),
    val selectedRelease: Release?     = null,
    val isLoading: Boolean            = false,
    val isSyncing: Boolean            = false,
    val error: String?                = null,
)
```

- [ ] **Step 2: Create ScheduleViewModel.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.domain.usecase.GetWeekReleasesUseCase
import com.kmno.dropdate.domain.usecase.SyncReleasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getWeekReleases: GetWeekReleasesUseCase,
    private val syncReleases: SyncReleasesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state
                .map { it.selectedWeekStart to it.activeFilter }
                .distinctUntilChanged()
                .flatMapLatest { (weekStart, filter) ->
                    _state.update { it.copy(isLoading = true) }
                    getWeekReleases(weekStart, weekStart.plusDays(6))
                        .map { releases -> releases.groupAndFilter(filter) }
                }
                .collectLatest { grouped ->
                    _state.update { it.copy(releases = grouped, isLoading = false) }
                }
        }
        onRefresh()
    }

    fun onDaySelected(day: LocalDate) {
        _state.update { it.copy(selectedDay = day) }
    }

    fun onWeekChanged(weekStart: LocalDate) {
        _state.update { it.copy(selectedWeekStart = weekStart) }
        onRefresh()
    }

    fun onFilterChanged(filter: ContentFilter) {
        _state.update { it.copy(activeFilter = filter) }
    }

    fun onReleaseSelected(release: Release) {
        _state.update { it.copy(selectedRelease = release) }
    }

    fun onSheetDismissed() {
        _state.update { it.copy(selectedRelease = null) }
    }

    fun onRefresh() {
        val weekStart = _state.value.selectedWeekStart
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, error = null) }
            syncReleases(weekStart, weekStart.plusDays(6))
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isSyncing = false) }
        }
    }

    private fun List<Release>.groupAndFilter(filter: ContentFilter): Map<LocalDate, List<Release>> {
        val filtered = when (filter) {
            ContentFilter.ALL    -> this
            ContentFilter.MOVIES -> filter { it.type == ReleaseType.MOVIE }
            ContentFilter.SERIES -> filter { it.type == ReleaseType.SERIES }
            ContentFilter.ANIME  -> filter { it.type == ReleaseType.ANIME }
        }
        return filtered.groupBy { it.airDate }
    }
}
```

- [ ] **Step 3: Write ViewModel tests**

Create `app/src/test/java/com/kmno/dropdate/presentation/schedule/ScheduleViewModelTest.kt`:

```kotlin
package com.kmno.dropdate.presentation.schedule

import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.domain.usecase.GetWeekReleasesUseCase
import com.kmno.dropdate.domain.usecase.SyncReleasesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

    private fun fakeRelease(id: String, type: ReleaseType, date: LocalDate) = Release(
        id = id, title = "T$id", posterUrl = null, backdropUrl = null,
        type = type, status = ReleaseStatus.UPCOMING,
        airDate = date, airTime = null, platform = null,
        episodeLabel = null, rating = null, synopsis = null,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(getWeekReleases(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(syncReleases).thenReturn(mock())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state has correct defaults`() {
        val vm = ScheduleViewModel(getWeekReleases, syncReleases)
        val state = vm.state.value
        assertEquals(ContentFilter.ALL, state.activeFilter)
        assertNull(state.selectedRelease)
        assertNull(state.error)
    }

    @Test
    fun `onFilterChanged updates activeFilter`() {
        val vm = ScheduleViewModel(getWeekReleases, syncReleases)
        vm.onFilterChanged(ContentFilter.ANIME)
        assertEquals(ContentFilter.ANIME, vm.state.value.activeFilter)
    }

    @Test
    fun `onReleaseSelected sets selectedRelease`() {
        val vm = ScheduleViewModel(getWeekReleases, syncReleases)
        val release = fakeRelease("1", ReleaseType.MOVIE, LocalDate.now())
        vm.onReleaseSelected(release)
        assertEquals(release, vm.state.value.selectedRelease)
    }

    @Test
    fun `onSheetDismissed clears selectedRelease`() {
        val vm = ScheduleViewModel(getWeekReleases, syncReleases)
        val release = fakeRelease("1", ReleaseType.MOVIE, LocalDate.now())
        vm.onReleaseSelected(release)
        vm.onSheetDismissed()
        assertNull(vm.state.value.selectedRelease)
    }

    @Test
    fun `onDaySelected updates selectedDay`() = runTest {
        val vm = ScheduleViewModel(getWeekReleases, syncReleases)
        val tuesday = LocalDate.now().with(java.time.DayOfWeek.TUESDAY)
        vm.onDaySelected(tuesday)
        assertEquals(tuesday, vm.state.value.selectedDay)
    }
}
```

- [ ] **Step 4: Run ViewModel tests**

Run: `gradlew.bat testDebugUnitTest --tests "*.presentation.schedule.*"`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/presentation/schedule/ScheduleUiState.kt app/src/main/java/com/kmno/dropdate/presentation/schedule/ScheduleViewModel.kt app/src/test/java/com/kmno/dropdate/presentation/schedule/
git commit -m "feat: add ScheduleUiState, ScheduleViewModel + tests"
```

---

### Task 14: UI components — WatchBadge + CountdownText

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/components/WatchBadge.kt`
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/components/CountdownText.kt`

- [ ] **Step 1: Create WatchBadge.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.ui.theme.ReleasedGreen
import com.kmno.dropdate.ui.theme.TextPrimary

@Composable
fun WatchBadge(platform: String?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "watchBadgeScale",
    )

    LaunchedEffect(Unit) { visible = true }

    Row(
        modifier = modifier
            .scale(scale)
            .background(ReleasedGreen, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(10.dp),
        )
        if (!platform.isNullOrBlank()) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = platform,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
```

- [ ] **Step 2: Create CountdownText.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun CountdownText(
    airDate: LocalDate,
    airTime: LocalTime?,
    modifier: Modifier = Modifier,
) {
    val targetEpoch = remember(airDate, airTime) {
        val time = airTime ?: LocalTime.of(23, 59, 59)
        ZonedDateTime.of(airDate, time, ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    }

    var remainingMs by remember { mutableLongStateOf(targetEpoch - System.currentTimeMillis()) }

    LaunchedEffect(targetEpoch) {
        while (remainingMs > 0) {
            delay(1_000)
            remainingMs = targetEpoch - System.currentTimeMillis()
        }
    }

    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val days    = (totalSeconds / 86400).toInt()
    val hours   = ((totalSeconds % 86400) / 3600).toInt()
    val minutes = ((totalSeconds % 3600) / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CountdownSegment(days, "d")
        Separator()
        CountdownSegment(hours, "h")
        Separator()
        CountdownSegment(minutes, "m")
        Separator()
        CountdownSegment(seconds, "s")
    }
}

@Composable
private fun CountdownSegment(value: Int, unit: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            slideInVertically { it } togetherWith slideOutVertically { -it }
        },
        label = "countdown_$unit",
    ) { v ->
        Text(
            text = "%02d".format(v),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = TextPrimary,
        )
    }
    Text(text = unit, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
}

@Composable
private fun Separator() {
    Text(text = " ", fontSize = 10.sp, color = TextSecondary)
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/presentation/schedule/components/WatchBadge.kt app/src/main/java/com/kmno/dropdate/presentation/schedule/components/CountdownText.kt
git commit -m "feat: add WatchBadge (bouncy pop) and CountdownText (digit roll animation)"
```

---

### Task 15: UI components — ReleaseCard

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/components/ReleaseCard.kt`

- [ ] **Step 1: Create ReleaseCard.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesBlue
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private val CardWidth  = 140.dp
private val CardHeight = 210.dp

@Composable
fun ReleaseCard(
    release: Release,
    index: Int,
    onClick: (Release) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 30L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.96f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "cardScale",
        )
        val accentColor = when (release.type) {
            ReleaseType.MOVIE  -> MovieAmber
            ReleaseType.SERIES -> SeriesBlue
            ReleaseType.ANIME  -> AnimePurple
        }

        Box(
            modifier = modifier
                .width(CardWidth)
                .height(CardHeight)
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onClick(release) },
                ),
        ) {
            // Poster image
            AsyncImage(
                model = release.posterUrl,
                contentDescription = release.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to Color.Transparent,
                            1f to Background,
                        )
                    )
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Text(
                    text = release.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )

                Spacer(Modifier.height(4.dp))

                // Rating + platform
                Row(verticalAlignment = Alignment.CenterVertically) {
                    release.rating?.let { r ->
                        Text(
                            text = "★ ${"%.1f".format(r)}",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    release.platform?.let { p ->
                        Text(
                            text = p,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                // Badge or countdown
                if (release.status == ReleaseStatus.RELEASED) {
                    WatchBadge(platform = release.platform)
                } else {
                    CountdownText(airDate = release.airDate, airTime = release.airTime)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/presentation/schedule/components/ReleaseCard.kt
git commit -m "feat: add ReleaseCard with poster, gradient scrim, rating, badge/countdown"
```

---

### Task 16: UI components — WeekScroller + ContentTypeChips

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/components/WeekScroller.kt`
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/components/ContentTypeChips.kt`

- [ ] **Step 1: Create WeekScroller.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.ui.theme.SeriesBlue
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DayItemWidth = 44.dp

@Composable
fun WeekScroller(
    weekStart: LocalDate,
    selectedDay: LocalDate,
    today: LocalDate = LocalDate.now(),
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val selectedIndex = days.indexOfFirst { it == selectedDay }.coerceAtLeast(0)

    val pillOffset by animateDpAsState(
        targetValue = DayItemWidth * selectedIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pillOffset",
    )

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Sliding pill background
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(DayItemWidth)
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SeriesBlue.copy(alpha = 0.2f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEachIndexed { _, day ->
                val isSelected = day == selectedDay
                val isToday = day == today
                Column(
                    modifier = Modifier
                        .width(DayItemWidth)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(day) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .take(3).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SeriesBlue else TextSecondary,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary,
                    )
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(SeriesBlue, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create ContentTypeChips.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.presentation.schedule.ContentFilter
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesBlue
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary

private val filters = listOf(
    ContentFilter.ALL,
    ContentFilter.MOVIES,
    ContentFilter.SERIES,
    ContentFilter.ANIME,
)

private fun ContentFilter.label() = when (this) {
    ContentFilter.ALL    -> "All"
    ContentFilter.MOVIES -> "Movies"
    ContentFilter.SERIES -> "Series"
    ContentFilter.ANIME  -> "Anime"
}

private fun ContentFilter.accentColor() = when (this) {
    ContentFilter.ALL    -> SeriesBlue
    ContentFilter.MOVIES -> MovieAmber
    ContentFilter.SERIES -> SeriesBlue
    ContentFilter.ANIME  -> AnimePurple
}

@Composable
fun ContentTypeChips(
    activeFilter: ContentFilter,
    onFilterSelected: (ContentFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = filters.indexOf(activeFilter).coerceAtLeast(0)
    val accentColor = activeFilter.accentColor()

    // Track each chip's width for the indicator offset
    val chipWidths = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateListOf<Float>() }
        .also { list -> repeat(filters.size - list.size) { list.add(0f) } }

    val density = LocalDensity.current
    val offsetDp by animateDpAsState(
        targetValue = with(density) { chipWidths.take(selectedIndex).sum().toDp() },
        animationSpec = tween(200),
        label = "chipIndicatorOffset",
    )
    val indicatorWidthDp by animateDpAsState(
        targetValue = with(density) { chipWidths.getOrElse(selectedIndex) { 0f }.toDp() },
        animationSpec = tween(200),
        label = "chipIndicatorWidth",
    )

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            filters.forEachIndexed { i, filter ->
                val isActive = filter == activeFilter
                val textColor by animateColorAsState(
                    targetValue = if (isActive) accentColor else TextSecondary,
                    animationSpec = tween(200),
                    label = "chipColor_$i",
                )
                Text(
                    text = filter.label(),
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .onGloballyPositioned { coords ->
                            chipWidths[i] = coords.size.width.toFloat()
                        },
                )
            }
        }

        // Sliding underline indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = offsetDp)
                .width(indicatorWidthDp)
                .height(2.dp)
                .background(accentColor, RoundedCornerShape(1.dp))
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/presentation/schedule/components/WeekScroller.kt app/src/main/java/com/kmno/dropdate/presentation/schedule/components/ContentTypeChips.kt
git commit -m "feat: add WeekScroller (animated pill) and ContentTypeChips (sliding underline)"
```

---

### Task 17: UI components — ReleaseSection

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/components/ReleaseSection.kt`

- [ ] **Step 1: Create ReleaseSection.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmno.dropdate.domain.model.Release

@Composable
fun ReleaseSection(
    title: String,
    accentColor: Color,
    releases: List<Release>,
    onReleaseClick: (Release) -> Unit,
    modifier: Modifier = Modifier,
) {
    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { headerVisible = true }

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn() + expandVertically(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = releases.size.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(releases, key = { _, r -> r.id }) { index, release ->
                ReleaseCard(
                    release = release,
                    index = index,
                    onClick = onReleaseClick,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/presentation/schedule/components/ReleaseSection.kt
git commit -m "feat: add ReleaseSection with animated header and lazy horizontal cards"
```

---

### Task 18: UI components — ReleaseDetailSheet

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/components/ReleaseDetailSheet.kt`

- [ ] **Step 1: Create ReleaseDetailSheet.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kmno.dropdate.domain.model.Release
import com.kmno.dropdate.domain.model.ReleaseStatus
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.ReleasedGreen
import com.kmno.dropdate.ui.theme.SeriesBlue
import com.kmno.dropdate.ui.theme.Surface
import com.kmno.dropdate.ui.theme.TextPrimary
import com.kmno.dropdate.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDetailSheet(
    release: Release,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentColor = when (release.type) {
        ReleaseType.MOVIE  -> MovieAmber
        ReleaseType.SERIES -> SeriesBlue
        ReleaseType.ANIME  -> AnimePurple
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            // Hero — blurred backdrop + poster
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                // Backdrop (blurred via heavy downscale)
                AsyncImage(
                    model = release.backdropUrl ?: release.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                // Dark scrim
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.4f),
                                1f to Background,
                            )
                        )
                )
                // Poster thumbnail
                AsyncImage(
                    model = release.posterUrl,
                    contentDescription = release.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                        .size(width = 90.dp, height = 130.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }

            // Metadata
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))

                // Title
                Text(
                    text = release.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )

                Spacer(Modifier.height(4.dp))

                // Type · Year · Episode
                val meta = buildString {
                    append(release.type.name.lowercase().replaceFirstChar { it.uppercase() })
                    release.airDate.year.let { append(" · $it") }
                    release.episodeLabel?.let { append(" · $it") }
                }
                Text(text = meta, fontSize = 13.sp, color = TextSecondary)

                Spacer(Modifier.height(8.dp))

                // Star rating
                release.rating?.let { rating ->
                    StarRating(rating = rating, accentColor = accentColor)
                    Spacer(Modifier.height(6.dp))
                }

                // Platform dot
                release.platform?.let { platform ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accentColor, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = platform, fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Synopsis
                release.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                    SynopsisSection(synopsis = synopsis)
                    Spacer(Modifier.height(16.dp))
                }

                // CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (release.status == ReleaseStatus.RELEASED) ReleasedGreen else Surface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (release.status == ReleaseStatus.RELEASED) {
                        Text("▶  Watch Now", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        CountdownText(airDate = release.airDate, airTime = release.airTime)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StarRating(rating: Float, accentColor: Color) {
    var animatedRating by remember { mutableStateOf(0f) }
    val displayRating by animateFloatAsState(
        targetValue = animatedRating,
        animationSpec = tween(durationMillis = 600),
        label = "starFill",
    )
    LaunchedEffect(Unit) { animatedRating = rating }

    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            val filled = (displayRating / 2f) > i
            Text(
                text = if (filled) "★" else "☆",
                color = if (filled) accentColor else TextSecondary,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${"%.1f".format(rating)} / 10",
            fontSize = 13.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SynopsisSection(synopsis: String) {
    var expanded by remember { mutableStateOf(false) }
    val preview = synopsis.take(180).let { if (synopsis.length > 180) "$it…" else it }

    Column {
        Text(
            text = "Synopsis",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
            },
            label = "synopsisExpand",
        ) { isExpanded ->
            Text(
                text = if (isExpanded) synopsis else preview,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 20.sp,
            )
        }

        if (synopsis.length > 180) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    text = if (expanded) "Show less ↑" else "Show more ↓",
                    color = SeriesBlue,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/presentation/schedule/components/ReleaseDetailSheet.kt
git commit -m "feat: add ReleaseDetailSheet with hero backdrop, star animation, synopsis expand"
```

---

### Task 19: ScheduleScreen — wire everything together

**Files:**
- Create: `app/src/main/java/com/kmno/dropdate/presentation/schedule/ScheduleScreen.kt`

- [ ] **Step 1: Create ScheduleScreen.kt**

```kotlin
package com.kmno.dropdate.presentation.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmno.dropdate.domain.model.ReleaseType
import com.kmno.dropdate.presentation.schedule.components.ContentTypeChips
import com.kmno.dropdate.presentation.schedule.components.ReleaseCard
import com.kmno.dropdate.presentation.schedule.components.ReleaseDetailSheet
import com.kmno.dropdate.presentation.schedule.components.ReleaseSection
import com.kmno.dropdate.presentation.schedule.components.WeekScroller
import com.kmno.dropdate.ui.theme.AnimePurple
import com.kmno.dropdate.ui.theme.Background
import com.kmno.dropdate.ui.theme.MovieAmber
import com.kmno.dropdate.ui.theme.SeriesBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Anchor scroll to selected day
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val sortedDates = remember(state.releases) { state.releases.keys.sorted() }
    val selectedDayIndex = remember(state.selectedDay, sortedDates) {
        sortedDates.indexOfFirst { it == state.selectedDay }.coerceAtLeast(0)
    }
    LaunchedEffect(state.selectedDay) {
        if (sortedDates.isNotEmpty()) lazyListState.animateScrollToItem(selectedDayIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        PullToRefreshBox(
            isRefreshing = state.isSyncing,
            onRefresh = viewModel::onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Week scroller — sticky header
                WeekScroller(
                    weekStart = state.selectedWeekStart,
                    selectedDay = state.selectedDay,
                    onDaySelected = viewModel::onDaySelected,
                )

                // Content type filter chips
                ContentTypeChips(
                    activeFilter = state.activeFilter,
                    onFilterSelected = viewModel::onFilterChanged,
                )

                Spacer(Modifier.height(8.dp))

                // Feed — animated on filter change
                AnimatedContent(
                    targetState = state.activeFilter,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                        (fadeOut() + slideOutHorizontally { -it / 4 })
                    },
                    label = "feedTransition",
                    modifier = Modifier.fillMaxSize(),
                ) { filter ->
                    if (filter == ContentFilter.ALL) {
                        // Category rows
                        val allReleases = state.releases.values.flatten()
                        val movies = allReleases.filter { it.type == ReleaseType.MOVIE }
                        val series = allReleases.filter { it.type == ReleaseType.SERIES }
                        val anime  = allReleases.filter { it.type == ReleaseType.ANIME }

                        LazyColumn(
                            state = lazyListState,
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (movies.isNotEmpty()) item {
                                ReleaseSection(
                                    title = "Movies",
                                    accentColor = MovieAmber,
                                    releases = movies,
                                    onReleaseClick = viewModel::onReleaseSelected,
                                )
                            }
                            if (series.isNotEmpty()) item {
                                ReleaseSection(
                                    title = "Series",
                                    accentColor = SeriesBlue,
                                    releases = series,
                                    onReleaseClick = viewModel::onReleaseSelected,
                                )
                            }
                            if (anime.isNotEmpty()) item {
                                ReleaseSection(
                                    title = "Anime",
                                    accentColor = AnimePurple,
                                    releases = anime,
                                    onReleaseClick = viewModel::onReleaseSelected,
                                )
                            }
                        }
                    } else {
                        // Flat vertical list
                        val flat = state.releases.values.flatten()
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(flat, key = { _, r -> r.id }) { index, release ->
                                ReleaseCard(
                                    release = release,
                                    index = index,
                                    onClick = viewModel::onReleaseSelected,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detail bottom sheet
        state.selectedRelease?.let { release ->
            ReleaseDetailSheet(
                release = release,
                onDismiss = viewModel::onSheetDismissed,
            )
        }
    }
}
```

- [ ] **Step 2: Full build and smoke test**

Run: `gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL — the app should compile with no unresolved references.

- [ ] **Step 3: Run all unit tests**

Run: `gradlew.bat testDebugUnitTest`
Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/kmno/dropdate/presentation/schedule/ScheduleScreen.kt
git commit -m "feat: wire ScheduleScreen — week scroller, filter chips, category rows, detail sheet"
```

---

### Task 20: Delete template code + final cleanup

**Files:**
- No longer needed: `Greeting` composable in `MainActivity.kt` (already replaced in Task 2)
- Verify: `ExampleUnitTest.kt` still compiles (no changes needed)

- [ ] **Step 1: Verify no template references remain**

Run:
```bash
grep -r "Greeting" app/src/main/java/
```
Expected: No output — `Greeting` was removed when `MainActivity` was rewritten in Task 2.

- [ ] **Step 2: Final build**

Run: `gradlew.bat assembleDebug lintDebug`
Expected: BUILD SUCCESSFUL. Lint may warn about unused resources from the template — these are non-blocking.

- [ ] **Step 3: Run full test suite**

Run: `gradlew.bat testDebugUnitTest`
Expected: All tests PASS.

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete DropDate clean architecture scaffold — MVVM, offline-first, 4 APIs"
```

---

## Self-Review Notes

**Spec coverage check:**
- ✅ Package structure (Section 3) — Tasks 1–20 create every file listed
- ✅ Domain model with `synopsis`, `backdropUrl` (Section 4.1) — Task 4
- ✅ Repository interface (Section 4.2) — Task 4
- ✅ Use cases + tests (Section 4.3) — Task 4
- ✅ AppError (Section 4.4) — Task 3
- ✅ Room entity + DAO (Section 5.1) — Task 5
- ✅ Retrofit setup + 4 interfaces (Sections 5.2–5.3) — Tasks 6–7
- ✅ ReleaseMapper + tests (Section 5.4) — Task 8
- ✅ ReleaseRepositoryImpl (Section 5.4) — Task 9
- ✅ Cache staleness (5-day policy) — Task 9, `deleteStale` call
- ✅ All 4 Hilt modules (Section 7) — Task 10
- ✅ Coil + shared OkHttpClient (Section 8) — Tasks 1–2
- ✅ Theme colors + typography (Section 6.1) — Task 11
- ✅ UiState + ViewModel with `flatMapLatest` fix (Section 6.2–6.3) — Task 13
- ✅ WeekScroller + animated pill (Section 6.5) — Task 16
- ✅ ContentTypeChips + sliding underline (Section 6.5) — Task 16
- ✅ ReleaseCard with scrim, rating, platform, staggered entry (Section 6.5) — Task 15
- ✅ WatchBadge bouncy pop (Section 6.5) — Task 14
- ✅ CountdownText digit roll (Section 6.5) — Task 14
- ✅ ReleaseSection animated header (Section 6.4) — Task 17
- ✅ ReleaseDetailSheet — hero, poster, stars, synopsis expand, CTA (Section 6.6) — Task 18
- ✅ AnimatedContent feed transition (Section 6.5) — Task 19
- ✅ Navigation scaffold (Section 6.7) — Task 12
- ✅ MainActivity with splash + edge-to-edge (Section 6.8) — Task 2

**Type consistency verified:** `groupAndFilter` defined in `ScheduleViewModel` (Task 13), called nowhere else. `ReleaseMapper` methods (`fromTmdb`, `fromTvMaze`, `fromJikan`, `toDomain`) match signatures used in `ReleaseRepositoryImpl` (Task 9). All `Release` fields match between `ReleaseEntity` ↔ `ReleaseMapper` ↔ `Release` domain model.
