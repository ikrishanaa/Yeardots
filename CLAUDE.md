# YearDots — Agent Context

> This file gives AI coding assistants (Claude Code, Cursor, Copilot, etc.)
> the context they need to work on this project safely and correctly.

## Project Overview

**YearDots** is a minimal Android app that generates a lock-screen wallpaper
showing the year's progress as a grid of dots. One dot = one day.
Past days, today, and future days each get a distinct colour.

- **Package**: `com.krishana.onedot`
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Persistence | DataStore Preferences |
| Background | WorkManager (CoroutineWorker) |
| Image Loading | Coil |
| Build | Gradle Kotlin DSL, AGP |
| CI/CD | GitHub Actions |

## Architecture — Source Map

```
app/src/main/java/com/krishana/onedot/
├── MainActivity.kt              # Single-activity entry point, all Compose UI
├── core/
│   └── WallpaperGenerator.kt    # Canvas-based bitmap renderer (365/366 dots)
├── data/
│   └── SettingsRepository.kt    # DataStore wrapper for all user preferences
├── receiver/
│   └── BootReceiver.kt          # Re-schedules WorkManager after reboot
├── ui/
│   ├── components/
│   │   ├── AboutDialog.kt       # About screen with social links
│   │   ├── ColorSettings.kt     # Colour picker UI
│   │   ├── LayoutEditorScreen.kt# Interactive grid resize editor
│   │   ├── ShapeSelector.kt     # Dot shape selector (circle/square/rounded/pill)
│   │   └── WallpaperPreview.kt  # Live wallpaper preview
│   └── theme/
│       ├── Theme.kt             # Material 3 theme definition
│       └── Type.kt              # Typography
├── util/
│   └── WorkScheduler.kt         # WorkManager scheduling (daily + immediate)
└── worker/
    └── WallpaperWorker.kt       # Background worker that generates & sets wallpaper
```

## Critical Rules

### 1. Day Calculation — DO NOT change the approach

```kotlin
val daysInYear     = Year.of(today.year).length()   // 365 or 366
val currentDayOfYear = today.dayOfYear              // 1-based
val daysLeft       = daysInYear - currentDayOfYear
```

- **Always** use `Year.length()` — never manual `isLeapYear` checks.
- **Always** use `today.dayOfYear` — never `ChronoUnit.DAYS.between() + 1`.
- The text on the wallpaper must say **"X days left"**, not just "X days".

### 2. WorkManager Timing

The daily wallpaper update targets **01:00 AM** with a 60-minute flex window.
This ensures the execution window is `[00:00, 01:00]` — entirely after midnight.

**Never** set the target to `00:01` or earlier — a flex window crossing midnight
causes `LocalDate.now()` to return yesterday's date.

### 3. Gradle / CI

- `org.gradle.java.home` must **NOT** be in the project's `gradle.properties`.
  It belongs in `~/.gradle/gradle.properties` (user-level, not committed).
- Both `.github/workflows/build.yml` and `release.yml` use `actions/setup-java`
  with JDK 17 Temurin. No `-D` overrides needed.

### 4. Adaptive Icons

- Icon source SVGs live in `assets/icon.svg` and `assets/icon_foreground.svg`.
- The `viewBox` is set to `-160 -160 832 832` for proper padding.
- Raster PNGs in `mipmap-*` are generated via a Node.js/resvg-js pipeline.
- **Do not** load adaptive icon XML via `painterResource()` in Compose — it crashes.
  The About dialog uses a manual `Box` + `Brush` + `Image` composition instead.

### 5. Wallpaper Quality

- Use `Bitmap.CompressFormat.PNG` at quality `100` (lossless).
- Use `WallpaperManager.setStream()` with `FLAG_LOCK` (lock screen only).
- Never use `setBitmap()` — it compresses and degrades quality.

## Build Commands

```bash
./gradlew assembleDebug          # Debug build
./gradlew assembleRelease        # Release build (needs signing config)
./gradlew test                   # Unit tests
```

## Version Bumping

Edit `app/build.gradle.kts`:
- `versionCode` — integer, increment by 1 each release
- `versionName` — semver string (e.g. "2.0.1")

Always update `CHANGELOG.md` with a new `## [X.Y.Z]` section **before** tagging.

## Release Process

```bash
git tag vX.Y.Z
git push origin main --tags
```

The `release.yml` workflow extracts notes from `CHANGELOG.md` using the version
number and creates a GitHub Release with signed APKs.

## Files to Never Commit

- `local.properties` (contains SDK path)
- `~/.gradle/gradle.properties` (contains local JDK path)
- `release.keystore` (signing key)
- `*.apk` files
