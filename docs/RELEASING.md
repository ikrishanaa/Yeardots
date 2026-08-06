# How to Release a New Version

This guide explains how to release a new version of the Year Dots app (e.g., 1.1, 1.2, 1.5) using the existing GitHub Actions workflow.

## 1. Update Version in Code

Open `app/build.gradle.kts` and update the `versionCode` and `versionName` inside the `defaultConfig` block.

```kotlin
defaultConfig {
    // ...
    versionCode = 2       // Increment this by 1 for every new release
    versionName = "1.1"   // The visible version name (e.g., 1.1, 1.2, 2.0)
    // ...
}
```

*   **versionCode**: An integer that must always increase (e.g., 1 -> 2).
*   **versionName**: A string representing the version (e.g., "1.1").

## 2. Update Changelog

Open `CHANGELOG.md` and add a new entry for the version you are releasing at the top of the history.

```markdown
## [1.1] - YYYY-MM-DD

### Added
- New feature description...

### Fixed
- Bug fix description...
```

## 3. Commit and Push

Commit these changes to the repository.

```bash
git add app/build.gradle.kts CHANGELOG.md
git commit -m "Prepare release 1.1"
git push
```

## 4. Trigger the Release

You have two ways to trigger the release workflow on GitHub:

### Option A: Using Git Tags (Recommended)

Simply create a tag starting with `v` matching your version name and push it.

```bash
git tag v1.1
git push origin v1.1
```

This will automatically:
1.  Trigger the `Release Build` workflow.
2.  Build the APK.
3.  Sign it (requires secrets configured).
4.  Create a **GitHub Release** with the APK attached.

### Option B: Manual Trigger via GitHub UI

1.  Go to the **Actions** tab in your GitHub repository.
2.  Select **Release Build** from the left sidebar.
3.  Click **Run workflow**.
4.  Enter the version number (e.g., `1.1`).
5.  Click **Run workflow**.

## 5. Download the Release

Once the workflow finishes (takes about 2-3 minutes), go to the **Releases** section on the main page of your GitHub repository. You will find the new release `v1.1` with `YearDots-1.1.apk` ready for download.

## Important Note on Signing

By default, the APK generated will be **unsigned** unless you have configured the following **Repository Secrets** in GitHub Settings -> Secrets and variables -> Actions:

*   `KEYSTORE_BASE64`: Your keystore file encoded in base64.
*   `KEYSTORE_PASSWORD`: The password for your keystore.
*   `KEY_ALIAS`: The alias of your signing key.
*   `KEY_PASSWORD`: The password for your specific key.

If these are not set, you can still install the debug APK or the unsigned release APK (requires bundle tool or signer), but for a proper release, signing is recommended.
