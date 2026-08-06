# Year Dots Release Guide

This document explains how to create and publish a new release of Year Dots.

## Prerequisites

- Maintainer access to the repository
- Release keystore configured in GitHub Secrets
- Updated CHANGELOG.md
- All tests passing

## Release Checklist

### 1. Prepare the Release

- [ ] Ensure `main` branch is stable and all CI checks pass
- [ ] Update version in `app/build.gradle.kts`:
  - Increment `versionCode`
  - Update `versionName` (follow semantic versioning)
- [ ] Update `CHANGELOG.md`:
  - Move items from "Unreleased" to new version section
  - Add release date
  - Verify all changes are documented
- [ ] Commit version bump:
  ```bash
  git add app/build.gradle.kts CHANGELOG.md
  git commit -m "chore: bump version to X.Y.Z"
  git push origin main
  ```

### 2. Create and Push Tag

```bash
# Create annotated tag
git tag -a vX.Y.Z -m "Release vX.Y.Z"

# Push tag to trigger release workflow
git push origin vX.Y.Z
```

### 3. Automated Release Process

GitHub Actions will automatically:
1. Build the release APK
2. Sign the APK (if keystore secrets are configured)
3. Extract changelog for this version
4. Create GitHub release with APK attached

Monitor the workflow at: `Actions` → `Release Build`

### 4. Post-Release Tasks

- [ ] Verify the release appears on GitHub with correct APK
- [ ] Test the release APK on a physical device
- [ ] Update F-Droid metadata (if applicable)
- [ ] Announce release (optional):
  - Reddit: r/Android, r/androidapps
  - XDA Forums
  - Social media

## Semantic Versioning Guidelines

Year Dots follows [Semantic Versioning](https://semver.org/):

**MAJOR.MINOR.PATCH** (e.g., 1.2.3)

- **MAJOR**: Breaking changes (e.g., minimum Android version bump)
- **MINOR**: New features (backwards-compatible)
- **PATCH**: Bug fixes and minor improvements

### Examples

- `1.0.0` → `1.0.1`: Fixed pill alignment bug
- `1.0.0` → `1.1.0`: Added widget support
- `1.5.0` → `2.0.0`: Raised minimum SDK to Android 10

## Hotfix Release Process

For critical bugs in production:

1. Create hotfix branch from release tag:
   ```bash
   git checkout -b hotfix/vX.Y.Z vX.Y.Z
   ```

2. Fix the bug and test thoroughly

3. Update version to X.Y.(Z+1) and CHANGELOG

4. Merge to main:
   ```bash
   git checkout main
   git merge hotfix/vX.Y.Z
   ```

5. Tag and release as normal

## Rollback Procedure

If a release has critical issues:

1. Mark the release as "Pre-release" on GitHub
2. Add a warning to the release notes
3. Prepare and release a hotfix version immediately
4. Once hotfix is verified, delete the problematic release

## Release Signing Setup

### Generate Keystore (One-time)

```bash
keytool -genkey -v -keystore year-dots-release.keystore \
  -alias year-dots -keyalg RSA -keysize 2048 -validity 10000
```

**IMPORTANT:** Store the keystore and passwords securely!

### Configure GitHub Secrets

Add these secrets to your repository:

1. `KEYSTORE_BASE64`: Base64-encoded keystore file
   ```bash
   base64 -w 0 year-dots-release.keystore
   ```

2. `KEYSTORE_PASSWORD`: Keystore password

3. `KEY_ALIAS`: Key alias (e.g., "year-dots")

4. `KEY_PASSWORD`: Key password

Navigate to: Repository Settings → Secrets and variables → Actions → New repository secret

## Troubleshooting

### Release workflow fails
- Check that all secrets are configured correctly
- Verify CHANGELOG.md has section for the version
- Ensure build.gradle.kts version matches tag

### APK won't install
- Verify APK is properly signed
- Check that `versionCode` is higher than previous release

### GitHub release not created
- Ensure GitHub token has release permissions
- Verify tag follows `v*` pattern

## Manual Release (Fallback)

If GitHub Actions fails, build manually:

```bash
# Build release APK
./gradlew assembleRelease

# Sign APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore year-dots-release.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  year-dots

# Align APK (optional but recommended)
zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk \
  YearDots-vX.Y.Z.apk

# Create GitHub release manually via web UI
```

## Version History

| Version | Date       | Highlights |
|---------|------------|------------|
| 1.0.0   | 2026-02-01 | Initial public release |

---

## See Also

- **[PUBLISHING.md](PUBLISHING.md)** - Guide for publishing to F-Droid and other platforms
- **[CHANGELOG.md](CHANGELOG.md)** - Detailed version history

---

**Questions?** Open an issue or contact maintainers.
