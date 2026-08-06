# GitHub Copilot Instructions for YearDots

## Project Context
This is an Android app (Kotlin + Jetpack Compose) that generates a lock-screen
wallpaper with 365 dots representing the year's progress.

## Code Style
- Kotlin-first, no Java
- Jetpack Compose for all UI
- Material 3 design system
- Coroutines + Flow for async
- DataStore for preferences
- WorkManager for background tasks

## Important Patterns

### Day Calculation (WallpaperGenerator.kt)
Always use:
```kotlin
val daysInYear = Year.of(today.year).length()
val currentDayOfYear = today.dayOfYear
```
Never use `ChronoUnit.DAYS.between()` or manual leap year checks.

### WorkManager Scheduling (WorkScheduler.kt)
Target time must be 01:00 AM or later to keep the flex window after midnight.

### Wallpaper Rendering
- Use `Bitmap.CompressFormat.PNG` at quality 100
- Use `WallpaperManager.setStream()` with `FLAG_LOCK`
- Never use `setBitmap()`

### Adaptive Icons
- Do not load adaptive icon XML via `painterResource()` in Compose
- Use manual Box + Brush + Image composition instead

## Build
- `./gradlew assembleDebug` for debug builds
- JDK 17 required
- `org.gradle.java.home` goes in `~/.gradle/gradle.properties`, not project level
