# DropDate 📅

DropDate is a modern Android application designed to help users track upcoming releases for Movies, TV Series, and Anime. Built with a focus on a smooth user experience, it features an offline-first architecture, real-time countdowns, and a clean, gesture-driven interface.

## ✨ Features

- **Weekly Schedule Navigation**: A unique floating "Week Scroller" that allows you to jump between dates with ease.
- **Content Filtering**: Quickly toggle between Movies, Series, and Anime using interactive chips.
- **Real-time Countdowns**: See exactly how long remains until the next episode or premiere.
- **Offline-First**: All data is cached locally using Room, ensuring you can check your schedule even without an internet connection.
- **Detailed Information**: High-quality posters, synopses, ratings, and streaming platform badges.
- **Background Sync**: Uses WorkManager to keep release data up-to-date in the background.
- **Modern UI**: Pure Jetpack Compose with Material 3, featuring custom animations and a dark-themed aesthetic.

## 🛠 Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Local Storage**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
- **Image Loading**: [Coil 3](https://coil-kt.github.io/coil/)
- **Background Work**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **JSON Parsing**: [Kotlin Serialization](https://kotlinlang.org/docs/serialization.html)
- **Concurrency**: Kotlin Coroutines & Flow

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 24 (Min SDK) / 36 (Target SDK)

### Configuration

The app requires a TMDB API key to fetch movie and series data. 

1. Create a `keystore.properties` file in your root directory (optional for release builds).
2. Add your API key to your `local.properties` or define it in your environment:
   ```properties
   TMDB_API_KEY=your_api_key_here
   ```

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/DropDate.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run the `:app` module.

## 🏗 Project Structure

- `data/`: Implementation of repositories, local DB (Room), and remote API (Retrofit).
- `domain/`: Business logic, domain models, and repository interfaces.
- `presentation/`: UI layer (Compose Screens, ViewModels, Components).
- `di/`: Dependency injection modules.
- `ui/theme/`: Global styling, colors, and dimensions.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
