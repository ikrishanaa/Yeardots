# Year Dots - CI/CD Workflow Guide

## 🚀 Automated Testing with GitHub Actions

This project includes a complete CI/CD pipeline that automatically runs whenever you push code to GitHub or create a Pull Request.

### How It Works

1. **Trigger Events**
   - Push to `main` or `master` branch
   - Any Pull Request targeting these branches

2. **Automated Steps**
   - ✅ Checks out your code
   - ✅ Sets up JDK 17 with caching
   - ✅ Builds the debug APK
   - ✅ Runs all unit tests (including the 15 new test cases)
   - ✅ Uploads test reports as artifacts
   - ✅ Uploads the built APK for download

3. **Where to See Results**
   - Go to your GitHub repo → **Actions** tab
   - Click on any workflow run to see detailed logs
   - Download test reports and APK from the "Artifacts" section

### Test Coverage

The workflow runs all test categories:

| Category | Tests | What They Verify |
|----------|-------|------------------|
| **Leap Year Logic** | 4 | Correct day calculation for 366-day years |
| **Memory Management** | 2 | Paint object reuse, no leaks |
| **Resource Handling** | 3 | Bitmap recycling, stream closing |
| **Data Persistence** | 3 | Settings save/load correctly |
| **Worker Reliability** | 2 | Background tasks handle errors |
| **Scheduler Config** | 1 | Flex interval ≥60 minutes |

### Manual Testing Commands

Before pushing, run locally:

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "WallpaperGeneratorTest"

# Build APK
./gradlew assembleDebug

# View test report in browser
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Badge for README

Add this to your `README.md` to show build status:

```markdown
[![Android CI](https://github.com/YOUR_USERNAME/year-dots/actions/workflows/android-ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/year-dots/actions/workflows/android-ci.yml)
```

### Troubleshooting

**Workflow fails?**
1. Check the "Annotations" tab in GitHub Actions for compilation errors
2. Download test reports artifact to see which specific tests failed
3. Run tests locally with `./gradlew testDebugUnitTest --info` for detailed logs

**Tests pass locally but fail on GitHub?**
- Ensure all files are committed (check `.gitignore`)
- Verify Gradle wrapper is included (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
- Check Java version compatibility (workflow uses JDK 17)

### Next Steps

After confirming tests pass:
1. Enable branch protection rules in GitHub Settings
2. Require status checks to pass before merging PRs
3. Consider adding release workflows for automatic APK publishing
