---
name: yeardots-android-development
description: Critical context and guidelines for working on the YearDots Android application.
---

# YearDots Android Development

This skill provides the necessary context and strict guidelines for modifying the **YearDots** Android application. YearDots is a minimalistic lock-screen wallpaper app that visually represents the year's progress using 365/366 dots.

## 1. Core Architecture
- **UI Framework**: Pure Jetpack Compose with Material 3. No XML layouts or Fragments. All UI is orchestrated in `MainActivity.kt`.
- **Background Tasks**: `WorkManager` (specifically `CoroutineWorker`) is used for the daily background wallpaper generation.
- **Persistence**: `DataStore Preferences` is used for all user settings.
- **Graphics**: `Canvas` API for bitmap generation. `Coil` for image loading.

## 2. Critical Mathematical Rules (NEVER DEVIATE)
When calculating days for the wallpaper, you must use the standard `java.time` APIs perfectly to avoid off-by-one errors and leap-year bugs.

### Leap Year & Total Days
**DO NOT** write custom modulo logic (`year % 4`).
**DO NOT** use `isLeapYear`.
**DO USE**:
```kotlin
val daysInYear = Year.of(today.year).length() // Returns 365 or 366 automatically
```

### Current Day & Days Left
**DO NOT** use `ChronoUnit.DAYS.between()`.
**DO USE**:
```kotlin
val currentDayOfYear = today.dayOfYear // 1-based index (1 to 365/366)
val daysLeft = daysInYear - currentDayOfYear
```

### String Formatting
Always use the format: `"$daysLeft days left • $percent% done"` to avoid ambiguity.

## 3. Wallpaper Service Rules
Android compresses Bitmaps heavily if the wrong API is used. To maintain pixel-perfect dots:
- **ALWAYS** use `Bitmap.CompressFormat.PNG` at `100` quality.
- **ALWAYS** set the wallpaper using an `InputStream` and `WallpaperManager.setStream()`.
- **NEVER** use `WallpaperManager.setBitmap()`, as it forces JPEG compression and degrades quality.
- **ALWAYS** use the `FLAG_LOCK` flag to only set the lock screen wallpaper.

## 4. WorkManager Timing Rules
The `WallpaperWorker` runs daily to update the wallpaper.
- The target time must be **01:00 AM** or later.
- **Why?** WorkManager has a minimum flex window of 60 minutes. If scheduled at 00:01 AM, the flex window becomes `[23:01, 00:01]`. If the worker fires before midnight, `LocalDate.now()` retrieves yesterday's date, causing the wallpaper to lag by a day. A 01:00 AM target ensures the `[00:00, 01:00]` window falls entirely in the correct day.

## 5. Build System & CI
- The project uses Gradle Kotlin DSL (`build.gradle.kts`).
- GitHub Actions CI is configured in `.github/workflows/build.yml` and `release.yml`.
- **NEVER** add `org.gradle.java.home` to the project's `gradle.properties`. It breaks the CI runners. JDK paths belong strictly in local user configurations (`~/.gradle/gradle.properties`).

## 6. AI Agent Learnings & Best Practices
Over the course of developing this app, several specific Android edge cases were discovered. AI agents MUST adhere to these rules when refactoring or adding features:

- **Adaptive Icons in Compose**: Do NOT use `painterResource` to load XML Adaptive Icons (like `ic_launcher.xml`) into a Compose UI element. It will crash the app on certain Android versions. Instead, build the UI element manually using standard Compose shapes, colors, and raster images.
- **DisplayMetrics Deprecation**: When calculating screen sizes for full-bleed wallpapers, avoid `DisplayMetrics`. Prefer using the exact `desiredMinimumWidth` and `desiredMinimumHeight` provided by `WallpaperManager`. If fallback is needed, use `WindowMetrics`.
- **Scaling Artifacts**: Never artificially scale up resolution (e.g., `width * 1.5f`) hoping to improve quality. This creates blurriness when Android downscales it. Generate the bitmap at the exact native dimensions requested by the OS.
- **State Hoisting**: The `LayoutEditorScreen` uses complex drag-and-drop mechanics. Always maintain state hoisting; never bury state inside deep child components to ensure the layout editor remains reactive to Spring physics.
- **Repository Hygiene**: Do not leave test or scratch files (`test_algo.kt`, `CheckSegmentedIcon.kt`) in the root or package structure. Move them to a scratch directory or delete them before committing.
