# YAMP - Yet Another Music Player

A modern Android music player with intelligent recommendations, auto-metadata lookup, and a clean neo-dark UI.

## Features

### Core
- **Universal Format Support**: MP3, FLAC, AAC, OGG Vorbis, WAV, OPUS, WMA, M4A via Media3/ExoPlayer
- **Automatic Library Scanning**: Scans device via MediaStore, organizes by folder structure into auto-generated playlists
- **Search & Sort**: Full-text search across title/artist/album/genre with sortable columns
- **Background Playback**: Media3 MediaSessionService with notification controls, Bluetooth/headset integration
- **Neo Dark Theme**: Ergonomic dark UI with modern curves, purple/cyan accent palette

### Advanced
- **Recommended Next**: History-based recommendation engine using play count, completion rate, time-of-day patterns, genre affinity, and skip analysis
- **Auto-Metadata Fetch**: Optional MusicBrainz integration to fill in missing artist/album/genre/year for incomplete tracks
- **Music Ontology**: In-memory knowledge graph connecting tracks, artists, albums, genres, and folders for enhanced discovery

## Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Audio**: Media3 (ExoPlayer) with MediaSessionService
- **Database**: Room with Flow-based reactive queries
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp (MusicBrainz API)
- **Architecture**: MVVM with clean architecture layers (data/domain/ui)
- **Testing**: JUnit 4 + MockK + Truth + Turbine

## Project Structure

```
com.yamp/
├── data/          # Room DB, DAOs, entities, repositories, scanner, remote APIs
├── domain/        # Models, use cases, ontology
├── player/        # ExoPlayer playback manager, service, queue
├── recommendation/# History-based recommendation engine
├── ui/            # Compose screens, components, theme, navigation
└── di/            # Hilt dependency injection modules
```

## Building

### Prerequisites
- Android SDK (API 35)
- JDK 17+
- Gradle 8.11+

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Version Bump + Release
```bash
./scripts/build-release.sh patch   # 1.0.0 -> 1.0.1
./scripts/build-release.sh minor   # 1.0.0 -> 1.1.0
./scripts/build-release.sh major   # 1.0.0 -> 2.0.0
```

### Run Tests
```bash
./gradlew testDebugUnitTest
```

## Supported Audio Formats

| Format | Extension | MIME Type |
|--------|-----------|-----------|
| MP3 | .mp3 | audio/mpeg |
| FLAC | .flac | audio/flac |
| AAC | .aac, .m4a | audio/aac, audio/mp4 |
| OGG Vorbis | .ogg | audio/ogg |
| WAV | .wav | audio/wav |
| OPUS | .opus | audio/opus |
| WMA | .wma | audio/x-ms-wma |

## Recommendation Algorithm

The engine scores candidates using weighted factors:
- **Play Count** (25%): Frequently played tracks score higher
- **Completion Rate** (20%): Tracks that are skipped less score higher
- **Time-of-Day** (20%): Matches listening patterns to current time
- **Genre Affinity** (20%): Prefers genres the user listens to most
- **Recency** (15%): Balances discovery with familiar preferences

Diversity is enforced by limiting per-artist results and excluding recently played tracks.

## License

MIT
