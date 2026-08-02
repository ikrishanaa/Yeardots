# Agent Guidelines for Year Dots Repository

## 📋 Project Overview
**Year Dots** is a native Android wallpaper application built with **Kotlin** and **Jetpack Compose**. It visualizes the progression of the year by rendering a grid of 365 (or 366) dots, highlighting the current day. The app runs entirely offline, updates automatically at midnight via WorkManager, and supports custom shapes, colors, and densities.

## 🏗️ Architecture & Tech Stack
- **Language**: Kotlin 1.9.22+
- **UI**: Jetpack Compose, Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Persistence**: Jetpack DataStore Preferences
- **Background Tasks**: WorkManager (daily updates)
- **Graphics**: Android Canvas (custom bitmap generation)
- **Dependency Injection**: Manual / Singleton (no heavy DI framework used)

### Key Modules
- `MainActivity`: Compose UI entry point, preview, and settings controls.
- `WallpaperGenerator`: Core logic for rendering the dot grid to a Bitmap.
- `SettingsRepository`: Manages user preferences (color, shape, density) via DataStore.
- `WallpaperWorker`: Background worker that triggers wallpaper generation and setting.
- `WorkScheduler`: Utility to schedule periodic and immediate wallpaper updates.

## 🐛 Critical Bug Fixes (Completed)
The following critical issues have been resolved:
1. **Leap Year Logic**: Fixed hardcoded `365` limit; now dynamically calculates `daysInYear` to support Dec 31st in leap years.
2. **Memory Leak**: Moved `Paint` object creation outside the rendering loop in `WallpaperGenerator`.
3. **Resource Leak**: Added proper `try-finally` blocks and `bitmap.recycle()` in `WallpaperWorker` to prevent OOM errors.
4. **Permission Logic**: Removed unnecessary `WRITE_EXTERNAL_STORAGE` permissions; app now relies solely on `SET_WALLPAPER`.
5. **Scheduler Reliability**: Increased WorkManager flex interval from 15m to 60m to ensure reliable daily execution.

## 🧪 Testing Strategy
### Test Execution
```bash
./gradlew testDebugUnitTest       # Run unit tests locally
./gradlew assembleDebug           # Build debug APK
./gradlew clean assembleDebug     # Clean build if caching issues occur
```

### Key Test Coverage
- **Logic**: Leap year date calculations, day-of-year indexing.
- **Memory**: Verification that Paint objects are reused, not recreated per frame.
- **Resources**: Ensuring Bitmaps and Streams are closed/recycled even on failure.
- **Worker**: Verifying WorkManager requests without fake `inputData` (uses `SettingsRepository`).
- **Scheduler**: Asserting flex intervals meet minimum thresholds (≥60 mins).

## 🔧 Build & Deployment
### Development Commands
```bash
./gradlew assembleDebug           # Build Debug APK
./gradlew assembleRelease         # Build Release APK (requires signing config)
./gradlew lint                    # Run static analysis
```

### CI/CD Workflow (GitHub Actions)
- Triggers on `push` and `pull_request`.
- Steps: Checkout → Setup JDK → Build Debug → Run Tests → Upload Artifacts.
- **Note**: Tests verify compilation and logic; no emulator required for unit tests.

## 📁 Important Files
- `app/src/main/java/com/krishana/onedot/MainActivity.kt`: UI & Interaction.
- `app/src/main/java/com/krishana/onedot/core/WallpaperGenerator.kt`: Rendering logic.
- `app/src/main/java/com/krishana/onedot/data/SettingsRepository.kt`: Data persistence.
- `app/src/main/java/com/krishana/onedot/work/WallpaperWorker.kt`: Background task.
- `app/src/test/java/.../YearDotsComprehensiveTest.kt`: Full test suite.
- `build.gradle.kts`: Module-level dependencies.

## ⚠️ CRITICAL OPERATIONAL RULES

### 1. 🛑 DO NOT MODIFY `.gitignore`
- **Rule**: Never change the `.gitignore` file unless explicitly instructed to fix a security breach involving exposed secrets.
- **Reason**: It protects sensitive files like `.env`, `local.properties`, and keystores. Accidental changes can leak credentials.
- **Action**: If a build artifact needs ignoring, check if it's already covered. If not, ask before modifying.

### 2. 🛑 Worker Input Data
- **Rule**: `WallpaperWorker` reads configuration directly from `SettingsRepository`.
- **Constraint**: Do **NOT** add fake `KEY_*` constants or pass `inputData` in tests.
- **Correct Pattern**:
  ```kotlin
  // ❌ WRONG
  val input = Data.Builder().putString("KEY_SHAPE", "dot").build()
  
  // ✅ CORRECT
  val request = OneTimeWorkRequestBuilder<WallpaperWorker>().build()
  // (Ensure SettingsRepository has the desired values before running worker)
  ```

### 3. 🛑 Memory Management
- Always recycle Bitmaps (`bitmap.recycle()`) after use in Workers.
- Reuse `Paint` and `MaskFilter` objects; never instantiate them inside loops.

## 🎯 Code Quality Standards
- **Null Safety**: Use Kotlin's safe calls (`?.`) and Elvis operator (`?:`).
- **Compose**: Keep UI stateless where possible; hoist state to ViewModel/Activity.
- **Naming**: Use descriptive names for constants (avoid magic numbers like `0.88f`).
- **Comments**: Document complex Canvas drawing logic and date calculations.

## 🔐 Security Notes
- **Permissions**: Only request `SET_WALLPAPER`. Do not request Storage permissions.
- **Secrets**: Store API keys (if any future integration) in `local.properties` or Environment Variables, never in code.
- **Exports**: Ensure generated wallpapers do not contain metadata leaking user location or device ID.

## 🚀 Future Enhancements
- Support for custom image backgrounds.
- Widget for home screen (in addition to lock screen).
- Export/Import settings presets.
- Animations for the "today" dot transition.
