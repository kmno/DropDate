# DropDate — Clean Architecture Design Spec
**Date:** 2026-04-18  
**Status:** Approved  
**Scope:** Initial architecture scaffold — single screen, no auth, offline-first

---

## 1. Overview

DropDate is a personal release tracker for movies, Western TV series, and anime. The first version shows the most popular titles from each data source as a default feed. Future versions will add search and a favourites/follow mechanism.

**Core constraints:**
- Offline-first — Room is the single source of truth; the UI always reads from Room, never directly from the network
- Single screen today, extensible to more screens without restructuring
- Minimal, high-contrast dark UI with fluid animations
- Four public APIs, no user authentication required

---

## 2. Data Sources

| Source | Coverage | Auth |
|---|---|---|
| [TMDB](https://api.themoviedb.org/3/) | Movies — upcoming, popular, metadata, posters, backdrops | Free API key (`BuildConfig.TMDB_API_KEY`) |
| [TVMaze](https://api.tvmaze.com/) | Western TV series — full schedule, streaming + broadcast, nextepisode | None |
| [Jikan](https://api.jikan.moe/v4/) | Anime — airing now, upcoming seasons, episode counts | None |
| [AnimeSchedule](https://animeschedule.net/api/v3/) | Anime — real-time airing, delay info, dub/sub dates, streaming platform links | None |

TMDB API key is stored in `local.properties` as `TMDB_API_KEY=…` and exposed via `BuildConfig`. It is never committed.

---

## 3. Package Structure

Single-module app. Approach: **layer-first data/domain, feature-grouped presentation**.

```
com.kmno.dropdate/
│
├── core/
│   ├── Result.kt                    # AppError sealed interface
│   └── ext/
│       └── FlowExt.kt               # .asResult(), stateIn helpers
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Room @Database
│   │   ├── dao/
│   │   │   └── ReleaseDao.kt
│   │   └── entity/
│   │       └── ReleaseEntity.kt
│   ├── remote/
│   │   ├── tmdb/
│   │   │   ├── TmdbApi.kt           # Retrofit interface
│   │   │   └── TmdbDto.kt
│   │   ├── tvmaze/
│   │   │   ├── TvMazeApi.kt
│   │   │   └── TvMazeDto.kt
│   │   ├── jikan/
│   │   │   ├── JikanApi.kt
│   │   │   └── JikanDto.kt
│   │   └── animeschedule/
│   │       ├── AnimeScheduleApi.kt
│   │       └── AnimeScheduleDto.kt
│   ├── repository/
│   │   └── ReleaseRepositoryImpl.kt
│   └── mapper/
│       └── ReleaseMapper.kt         # Dto→Entity, Entity↔Domain
│
├── domain/
│   ├── model/
│   │   ├── Release.kt
│   │   ├── ReleaseType.kt
│   │   └── ReleaseStatus.kt
│   ├── repository/
│   │   └── ReleaseRepository.kt     # interface
│   └── usecase/
│       ├── GetWeekReleasesUseCase.kt
│       └── SyncReleasesUseCase.kt
│
├── presentation/
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt                # @Serializable sealed class
│   └── schedule/
│       ├── ScheduleScreen.kt
│       ├── ScheduleViewModel.kt
│       ├── ScheduleUiState.kt
│       └── components/
│           ├── WeekScroller.kt
│           ├── ContentTypeChips.kt
│           ├── ReleaseSection.kt
│           ├── ReleaseCard.kt
│           ├── ReleaseDetailSheet.kt
│           ├── WatchBadge.kt
│           └── CountdownText.kt
│
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
│
├── DropDateApp.kt                   # @HiltAndroidApp + Coil ImageLoaderFactory
└── MainActivity.kt                  # @AndroidEntryPoint
```

**Dependency rules:**
- `domain` — pure Kotlin, zero Android/framework imports
- `data` depends on `domain` (implements interfaces, maps to domain models)
- `presentation` depends on `domain` only (never on `data`)
- `di` depends on all layers — wiring only, no business logic

---

## 4. Domain Layer

### 4.1 Domain Model

```kotlin
data class Release(
    val id: String,           // "{source}_{remoteId}" e.g. "tmdb_12345"
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?, // wide landscape — blurred hero in detail sheet
    val type: ReleaseType,
    val status: ReleaseStatus,
    val airDate: LocalDate,
    val airTime: LocalTime?,  // nullable — not all APIs provide time
    val platform: String?,    // "Netflix", "Crunchyroll", etc.
    val episodeLabel: String?,// "S02E05" for series/anime, null for movies
    val rating: Float?,       // 0–10
    val synopsis: String?,    // plot overview — shown in detail sheet
)

enum class ReleaseType   { MOVIE, SERIES, ANIME }
enum class ReleaseStatus { UPCOMING, RELEASED }
```

**Composite ID rationale:** AnimeSchedule and Jikan both cover anime — the source prefix prevents collisions when merging into one Room table.

**`airTime` nullable rationale:** TMDB upcoming movies carry a date but no time. TVMaze and AnimeSchedule provide precise times. Countdown uses it when present, falls back to end-of-day otherwise.

### 4.2 Repository Interface

```kotlin
interface ReleaseRepository {
    fun getReleasesForWeek(weekStart: LocalDate, weekEnd: LocalDate): Flow<List<Release>>
    suspend fun syncReleases(weekStart: LocalDate, weekEnd: LocalDate): Result<Unit>
}
```

### 4.3 Use Cases

```kotlin
class GetWeekReleasesUseCase(private val repository: ReleaseRepository) {
    operator fun invoke(weekStart: LocalDate, weekEnd: LocalDate): Flow<List<Release>> =
        repository.getReleasesForWeek(weekStart, weekEnd)
}

class SyncReleasesUseCase(private val repository: ReleaseRepository) {
    suspend operator fun invoke(weekStart: LocalDate, weekEnd: LocalDate): Result<Unit> =
        repository.syncReleases(weekStart, weekEnd)
}
```

### 4.4 Error Types

```kotlin
sealed interface AppError {
    data class Network(val message: String)  : AppError
    data class Database(val message: String) : AppError
    data object Unknown                      : AppError
}
```

---

## 5. Data Layer

### 5.1 Room

**Single flat table — no joins, fast queries:**

```kotlin
@Entity(tableName = "releases")
data class ReleaseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: String,          // ReleaseType.name()
    val status: String,        // ReleaseStatus.name()
    val airDate: String,       // ISO-8601 "2025-06-14"
    val airTime: String?,      // "21:00" or null
    val platform: String?,
    val episodeLabel: String?,
    val rating: Float?,
    val synopsis: String?,
    val syncedAt: Long         // epoch ms — drives staleness eviction
)

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

### 5.2 Retrofit Setup

One `OkHttpClient` shared across all Retrofit instances and Coil. One `Retrofit` instance per API base URL.

```kotlin
// NetworkModule — shared builder
private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(
            Json { ignoreUnknownKeys = true }
                .asConverterFactory("application/json".toMediaType())
        )
        .build()
```

### 5.3 Retrofit Service Interfaces

```kotlin
interface TmdbApi {
    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(@Query("api_key") key: String = BuildConfig.TMDB_API_KEY, @Query("page") page: Int = 1): TmdbMovieListDto

    @GET("movie/popular")
    suspend fun getPopularMovies(@Query("api_key") key: String = BuildConfig.TMDB_API_KEY): TmdbMovieListDto
}

interface TvMazeApi {
    @GET("schedule/web")
    suspend fun getStreamingSchedule(@Query("date") date: String): List<TvMazeScheduleEntryDto>

    @GET("schedule")
    suspend fun getBroadcastSchedule(@Query("date") date: String): List<TvMazeScheduleEntryDto>
}

interface JikanApi {
    @GET("seasons/now")
    suspend fun getCurrentlyAiringAnime(@Query("page") page: Int = 1): JikanSeasonDto

    @GET("seasons/upcoming")
    suspend fun getUpcomingAnime(): JikanSeasonDto
}

interface AnimeScheduleApi {
    @GET("timetables")
    suspend fun getTimetable(@Query("tz") timezone: String = "UTC"): List<AnimeScheduleEntryDto>
}
```

### 5.4 Repository Implementation

```kotlin
class ReleaseRepositoryImpl(...) : ReleaseRepository {

    override fun getReleasesForWeek(weekStart: LocalDate, weekEnd: LocalDate): Flow<List<Release>> =
        dao.observeByWeek(weekStart.toString(), weekEnd.toString())
           .map { it.map(mapper::toDomain) }

    override suspend fun syncReleases(weekStart: LocalDate, weekEnd: LocalDate): Result<Unit> =
        runCatching {
            coroutineScope {
                val movies        = async { tmdbApi.getUpcomingMovies() }
                val popular       = async { tmdbApi.getPopularMovies() }
                val streaming     = async { tvMazeApi.getStreamingSchedule(weekStart.toString()) }
                val broadcast     = async { tvMazeApi.getBroadcastSchedule(weekStart.toString()) }
                val airingAnime   = async { jikanApi.getCurrentlyAiringAnime() }
                val upcomingAnime = async { jikanApi.getUpcomingAnime() }
                val timetable     = async { animeScheduleApi.getTimetable() }

                val entities = buildList {
                    addAll(mapper.fromTmdb(movies.await(), popular.await()))
                    addAll(mapper.fromTvMaze(streaming.await(), broadcast.await()))
                    addAll(mapper.fromJikan(airingAnime.await(), upcomingAnime.await(), timetable.await()))
                }

                dao.upsertAll(entities)
                dao.deleteStale(System.currentTimeMillis() - 7.days.inWholeMilliseconds)
            }
        }
}
```

### 5.5 Cache Staleness Policy

| Scenario | Behaviour |
|---|---|
| App opens, Room has data | Emit cached immediately; sync in background |
| App opens, Room empty | Show shimmer; await first sync result |
| Sync fails | Keep showing cached data; surface snackbar error |
| Data older than 7 days | Deleted on next successful sync |

---

## 6. Presentation Layer

### 6.1 Visual Language

| Token | Value | Usage |
|---|---|---|
| Background | `#080810` | Screen background |
| Surface | `#12121E` | Card background |
| Movies accent | `#F5A623` | Amber |
| Series accent | `#4A9EFF` | Electric blue |
| Anime accent | `#C084FC` | Neon purple |
| Released badge | `#22C55E` | Green — Watch badge |
| Text primary | `#F1F1F5` | Titles |
| Text secondary | `#6B6B85` | Metadata |

Typography: system default (`Inter` preferred) — heavy weight for titles, monospace for countdown digits.

### 6.2 UI State

```kotlin
data class ScheduleUiState(
    val selectedWeekStart: LocalDate  = LocalDate.now().with(DayOfWeek.MONDAY),
    val selectedDay: LocalDate        = LocalDate.now(),
    val activeFilter: ContentFilter   = ContentFilter.ALL,
    val releases: Map<LocalDate, List<Release>> = emptyMap(), // pre-grouped, pre-filtered
    val selectedRelease: Release?     = null,  // non-null = detail sheet open
    val isLoading: Boolean            = false,
    val isSyncing: Boolean            = false,
    val error: String?                = null,
)

enum class ContentFilter { ALL, MOVIES, SERIES, ANIME }
```

`releases` is pre-grouped and pre-filtered in the ViewModel — composables render only, never sort or filter.

### 6.3 ViewModel

`groupAndFilter` is a private extension on `List<Release>` that groups items by `LocalDate` and, when filter ≠ ALL, keeps only the matching `ReleaseType`. It lives in `ScheduleViewModel.kt`.

```kotlin
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getWeekReleases: GetWeekReleasesUseCase,
    private val syncReleases: SyncReleasesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        // Reactive: re-subscribes to Room whenever weekStart or activeFilter changes.
        // flatMapLatest cancels the previous Room flow automatically.
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

    fun onDaySelected(day: LocalDate)          { _state.update { it.copy(selectedDay = day) } }
    fun onWeekChanged(weekStart: LocalDate)    { _state.update { it.copy(selectedWeekStart = weekStart) }; onRefresh() }
    fun onFilterChanged(filter: ContentFilter) { _state.update { it.copy(activeFilter = filter) } }
    fun onReleaseSelected(release: Release)    { _state.update { it.copy(selectedRelease = release) } }
    fun onSheetDismissed()                     { _state.update { it.copy(selectedRelease = null) } }

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
        val filtered = if (filter == ContentFilter.ALL) this
                       else filter { it.type.matchesFilter(filter) }
        return filtered.groupBy { it.airDate }
    }
}
```

### 6.4 Component Tree

```
ScheduleScreen  [SharedTransitionLayout wraps all]
├── WeekScroller             sticky header, HorizontalPager, animated pill selector
├── ContentTypeChips         sliding underline indicator between chips
└── AnimatedContent          cross-fades + slides on filter change
    ├── [ALL]
    │   ├── ReleaseSection("Movies")    LazyRow of ReleaseCards
    │   ├── ReleaseSection("Series")    LazyRow of ReleaseCards
    │   └── ReleaseSection("Anime")     LazyRow of ReleaseCards
    └── [MOVIES | SERIES | ANIME]
        └── LazyColumn of ReleaseCards  staggered entry animations

ReleaseDetailSheet           ModalBottomSheet — shown when selectedRelease != null
```

### 6.5 Animation Specs

#### WeekScroller
- Pill slides via `animateDpAsState(spring(dampingRatio = Spring.DampingRatioMediumBouncy))`
- Today: small accent dot, `AnimatedVisibility` fade-in

#### ContentTypeChips
- Sliding 2dp underline, offset via `animateDpAsState(tween(200))`
- Active chip text color: `animateColorAsState(tween(200))` to accent color

#### ReleaseCard (140×200dp)
- Coil `AsyncImage` with `crossfade(400)`
- Bottom gradient scrim: `0f→transparent, 0.4f→transparent, 1f→#080810`
- Shows: title, `★ rating`, platform pill, Watch badge or countdown
- Press: `scale(0.96f)` via `animateFloatAsState(spring(stiffness = MediumLow))`
- Entry: `fadeIn + slideInVertically`, staggered by `index × 30ms`
- Shared element key: `"poster_${release.id}"`

#### Filter Transition
```
fadeIn(tween(300)) + slideInHorizontally { it/4 }
    togetherWith
fadeOut(tween(200)) + slideOutHorizontally { -it/4 }
```

#### Shimmer Loading
- Skeleton cards matching ReleaseCard dimensions
- `rememberInfiniteTransition` drives left→right gradient sweep
- Replaced by `AnimatedVisibility(fadeOut)` when data arrives

#### WatchBadge
- `scaleIn(spring(dampingRatio = 0.5f))` on first appearance — bouncy pop

#### CountdownText (DD HH MM SS)
- Each digit: `AnimatedContent` with `slideInVertically + slideOutVertically` — mechanical counter roll
- Colon separators: alpha pulse via `rememberInfiniteTransition`

### 6.6 Detail Bottom Sheet

Triggered by tapping a `ReleaseCard`. Dismissed by swipe or tap outside.

**Layout:**
```
╔══════════════════════════════╗
║  Blurred backdrop (Coil      ║  backdropUrl + BlurTransformation(25f) + 60% dark scrim
║  BlurTransformation) + scrim ║  fadeIn(tween(400))
║   ┌─────────┐                ║
║   │ POSTER  │                ║  sharedElement morph from card — no fade, true position morph
║   │ 120×180 │                ║
║   └─────────┘                ║
╠══════════════════════════════╣
║  Title                Bold   ║  fadeIn delay 50ms
║  ANIME · 2025 · S01E04       ║  fadeIn delay 100ms
║  ★★★★☆  8.4 / 10            ║  stars fill left→right animateFloatAsState(tween(300))
║  ● Crunchyroll               ║  accent colour dot + platform name
╠══════════════════════════════╣
║  Synopsis (3 lines preview)  ║  fadeIn delay 150ms
║  [Show more ↓]               ║  AnimatedContent expandVertically + fadeIn
╠══════════════════════════════╣
║  [▶ WATCH NOW]               ║  if RELEASED — green CTA
║  [⏱ 02d 14h 32m 08s]        ║  if UPCOMING — live countdown
╚══════════════════════════════╝
```

**Sheet config:** `skipPartiallyExpanded = true`, `sheetMaxWidth = Dp.Unspecified` (full width).

**Shared element:** both `ReleaseCard` and `ReleaseDetailSheet` use the same `sharedElement(rememberSharedContentState("poster_${release.id}"), animatedVisibilityScope)` key inside `SharedTransitionLayout`.

### 6.7 Navigation

```kotlin
@Serializable sealed class Screen {
    @Serializable object Schedule : Screen()
    // Detail, Search, Settings — added as peers here
}

@Composable
fun DropDateNavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Schedule) {
        composable<Screen.Schedule> { ScheduleScreen(navController) }
    }
}
```

### 6.8 MainActivity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            DropDateTheme {
                DropDateNavGraph(rememberNavController())
            }
        }
    }
}
```

---

## 7. Dependency Injection

### AppModule
- `CoroutineScope` — `SupervisorJob() + Dispatchers.Default`
- `CoroutineDispatcher` — `Dispatchers.IO`

### DatabaseModule
- `AppDatabase` — Room, `fallbackToDestructiveMigration()` for v1
- `ReleaseDao`

### NetworkModule
- `OkHttpClient` — shared by all Retrofit instances and Coil; `HEADERS` logging in debug, `NONE` in release
- `TmdbApi`, `TvMazeApi`, `JikanApi`, `AnimeScheduleApi` — one Retrofit per base URL

### RepositoryModule
- `@Binds ReleaseRepositoryImpl → ReleaseRepository`

### Dependency Graph
```
SingletonComponent
├── OkHttpClient
│   ├── TmdbApi          → https://api.themoviedb.org/3/
│   ├── TvMazeApi        → https://api.tvmaze.com/
│   ├── JikanApi         → https://api.jikan.moe/v4/
│   ├── AnimeScheduleApi → https://animeschedule.net/api/v3/
│   └── Coil ImageLoader
├── AppDatabase → ReleaseDao
└── ReleaseRepositoryImpl (as ReleaseRepository)

ViewModelComponent
└── ScheduleViewModel
    ├── GetWeekReleasesUseCase
    └── SyncReleasesUseCase
```

---

## 8. Coil Setup

```kotlin
@HiltAndroidApp
class DropDateApp : Application(), ImageLoaderFactory {
    @Inject lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader() = ImageLoader.Builder(this)
        .networkObserverEnabled(true)
        .memoryCache { MemoryCache.Builder().maxSizePercent(this, 0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(50 * 1024 * 1024)
                .build()
        }
        .okHttpClient(okHttpClient)
        .build()
}
```

---

## 9. Dependencies Added (libs.versions.toml additions)

| Library | Version | Purpose |
|---|---|---|
| `coil-compose` | 3.1.0 | Compose image loading |
| `coil-network-okhttp` | 3.1.0 | Shares app OkHttpClient |
| `hilt-android` | 2.59.2 | DI |
| `room-runtime` + `room-ktx` | 2.8.4 | Local DB |
| `datastore-preferences` | 1.2.1 | Lightweight KV persistence |
| `work-runtime-ktx` | 2.11.2 | Background sync (ready for v2) |
| `navigation-compose` | 2.9.7 | Type-safe Compose nav |
| `retrofit` + converter | 3.0.0 | Network |
| `okhttp` + logging | 5.3.2 | HTTP client |
| `kotlinx-serialization-json` | 1.11.0 | JSON |
| `kotlinx-coroutines-android` | 1.10.2 | Async |
| `core-splashscreen` | 1.2.0 | Splash screen API |

---

## 10. Out of Scope (v1)

- User authentication
- Search functionality
- Favourites / follow mechanism
- Push notifications
- WorkManager periodic background sync
- Multi-module project structure
- Pagination (Paging 3) — all popular results fit in one page for v1
