# Contributing to Year Dots

First off, thank you for considering contributing to Year Dots! 🎉

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How Can I Contribute?

### 🐛 Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

**When filing a bug report, include:**
- **Clear title** describing the issue
- **Device info**: Android version, device model
- **Steps to reproduce** the behavior
- **Expected behavior** vs actual behavior
- **Screenshots** if applicable
- **Logs** if available (use `adb logcat`)

Use the [Bug Report template](https://github.com/ikrishanaa/Yeardots/issues/new?template=bug_report.yml).

### 💡 Suggesting Features

Feature requests are welcome! Please:
- Check if the feature has already been suggested
- Clearly describe the feature and its benefits
- Explain why this feature would be useful to most users
- Consider whether it fits the app's minimalist philosophy

Use the [Feature Request template](https://github.com/ikrishanaa/Yeardots/issues/new?template=feature_request.yml).

### 🔧 Pull Requests

1. **Fork the repository** and create your branch from `main`:
   ```bash
   git checkout -b feature/my-new-feature
   ```

2. **Make your changes:**
   - Follow the existing code style
   - Add comments for complex logic
   - Update documentation if needed

3. **Test your changes:**
   - Build the app: `./gradlew assembleDebug`
   - Test on physical device or emulator
   - Verify no new warnings in build output

4. **Commit your changes:**
   - Use clear, descriptive commit messages
   - Follow conventional commits format (see below)

5. **Push to your fork:**
   ```bash
   git push origin feature/my-new-feature
   ```

6. **Open a Pull Request** with:
   - Clear description of changes
   - Screenshots/GIFs for UI changes
   - Reference to related issue (if any)

## Development Setup

### Prerequisites
- Android Studio (latest stable)
- JDK 17+
- Android SDK with API 26+ (Android 8.0+)

### Building Locally
```bash
git clone https://github.com/ikrishanaa/Yeardots.git
cd year-dots
./gradlew assembleDebug
```

### Running Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Code Style Guidelines

### Kotlin
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Prefer immutability (`val` over `var`)
- Use data classes for simple data holders

### Compose
- Keep composables small and focused
- Use `remember` and `derivedStateOf` appropriately
- Avoid side effects in composition
- Extract reusable components to `ui/components/`

### Architecture
- **Repository pattern** for data access
- **ViewModel** not used (simple state management in MainActivity)
- **WorkManager** for background tasks
- **DataStore** for preferences

## Commit Message Format

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code formatting (no logic change)
- `refactor`: Code restructuring (no behavior change)
- `test`: Adding/updating tests
- `chore`: Build process, dependencies

**Examples:**
```
feat(wallpaper): add pill shape option

fix(preview): correct alignment of pill icon

docs(readme): add F-Droid installation instructions

chore(deps): update WorkManager to 2.9.0
```

## Branch Naming

- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation updates
- `refactor/description` - Code refactoring

## What We're Looking For

**Good First Issues:**
- Look for issues labeled `good first issue`
- Documentation improvements
- UI polish and animations
- Bug fixes

**Medium Complexity:**
- New wallpaper customization options
- Performance optimizations
- Accessibility improvements

**Advanced:**
- Widget implementation
- Alternative calendar systems
- Export functionality

## Questions?

Feel free to:
- Open a [discussion](https://github.com/ikrishanaa/Yeardots/discussions)
- Comment on existing issues
- Reach out to maintainers

---

Thank you for contributing! Every contribution, no matter how small, makes a difference. 🙌
