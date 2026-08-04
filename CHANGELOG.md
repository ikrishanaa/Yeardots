# Changelog

All notable changes to Year Dots will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Widget support for home screen
- Export wallpaper as image
- Multiple calendar systems (lunar, ISO week-based)
- Milestone markers for important dates

## [1.32.0] - 2026-08-04

### Added
- **Material 3 Premium UI Overhaul**: Transitioned all components to the modern Material 3 design system with dynamic, depth-focused `ElevatedCard` containers.
- **Fluid Layout Shift & Spring Physics**: Layout Editor grid now resizes smoothly with spring physics and calculates dot positions continuously during aspect ratio changes.
- **Segmented Button Controls**: Replaced static sliders and buttons with fluid, animated `SingleChoiceSegmentedButtonRow` controls for shape and size selection.
- **Animated Micro-interactions**: Added buttery smooth `animateColorAsState` transitions when selecting color palettes.

## [1.31.0] - 2026-08-04

### Added
- Pro Grid Layout Editor: Interactive 8-handle resizing for precise wallpaper alignment.
- Dynamic Responsive Grid: Dots now automatically scale and reshape via aspect ratio algorithms to maintain square cells.
- Centre-Horizontal alignment button for exact mid-screen centering.

### Fixed
- Added status bar padding in the editor to prevent overlapping with system icons.
- Cleaned up professional documentation in the repository.

## [1.30.4] - 2026-02-01

### Fixed
- Migrated signing logic to Gradle for standard, reliable V1+V2 signatures. This fixes the "app not installed" error permanently.

## [1.30.3] - 2026-02-01

### Fixed
- Fixed APK installation error on newer Android versions by switching to V2 signing scheme (apksigner).

## [1.30.2] - 2026-02-01

### Fixed
- Fixed issue where split APKs were not being correctly signed in the release workflow.

## [1.30.1] - 2026-02-01

### Added
- Multiple APK support (arm64, v7a, x86, universal) for optimized downloads.

## [1.0.0] - 2026-02-01

### Added
- Initial public release
- 365-dot calendar wallpaper with automatic daily updates
- Four dot shapes: Circle, Rounded, Square, Pill
- Four size densities: Tiny, Small, Medium, Large
- Full color customization (past, today, future, background)
- Live preview of wallpaper before applying
- AMOLED-friendly dark themes
- WorkManager integration for reliable midnight updates
- DataStore for persistent settings
- Boot receiver to reschedule updates after device restart
- About screen with app information and links

### Technical
- Built with Jetpack Compose and Material 3
- Kotlin-first architecture
- Fully offline operation
- No tracking or analytics
- MIT License

---

## Version History Format

### Types of Changes
- **Added** - New features
- **Changed** - Changes to existing functionality
- **Deprecated** - Soon-to-be removed features
- **Removed** - Removed features
- **Fixed** - Bug fixes
- **Security** - Security vulnerability fixes

---

[Unreleased]: https://github.com/ikrishanaa/Yeardots/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/ikrishanaa/Yeardots/releases/tag/v1.0.0
