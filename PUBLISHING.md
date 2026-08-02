# Year Dots Publishing Guide

This comprehensive guide explains how to version your app and publish it on F-Droid and other Android app distribution platforms.

---

## Table of Contents

1. [App Versioning](#app-versioning)
2. [Publishing on F-Droid](#publishing-on-f-droid)
3. [Publishing on IzzyOnDroid](#publishing-on-izzyondroid)
4. [Publishing on GitHub Releases](#publishing-on-github-releases)
5. [Alternative Distribution Platforms](#alternative-distribution-platforms)
6. [Fastlane Metadata Setup](#fastlane-metadata-setup)

---

## App Versioning

### Understanding Android Version Numbers

Android apps use two version identifiers in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 1        // Integer: Internal version (must increment each release)
    versionName = "1.0.0"  // String: User-visible version (follows semver)
}
```

- **`versionCode`**: An integer that must increase with every release. This is what Google Play, F-Droid, and other stores use to determine if an update is available.
- **`versionName`**: A string shown to users (e.g., "1.0.0", "1.2", "2.0-beta").

### Version Naming Conventions

#### Semantic Versioning (Recommended)

Format: `MAJOR.MINOR.PATCH` (e.g., `1.2.3`)

| Part   | When to Increment                                      | Example                    |
|--------|--------------------------------------------------------|----------------------------|
| MAJOR  | Breaking changes, major redesign, min SDK bump         | 1.0.0 → 2.0.0             |
| MINOR  | New features (backwards-compatible)                    | 1.0.0 → 1.1.0             |
| PATCH  | Bug fixes, minor improvements                          | 1.0.0 → 1.0.1             |

#### Alternative Formats

You can also use simpler formats:

| Format       | Examples                  | Use Case                         |
|--------------|---------------------------|----------------------------------|
| `X.Y`        | v1.0, v1.1, v1.2, v2.0   | Simple, user-friendly versions   |
| `X.YZ`       | v1.01, v1.02, v1.10      | Two-digit minor versioning       |
| `X.Y.Z`      | v1.0.0, v1.2.3           | Semantic versioning (recommended)|
| `X.Y.Z-tag`  | v1.0.0-beta, v2.0.0-rc1  | Pre-release versions             |

### Version Progression Examples

**For Year Dots:**

| versionCode | versionName | Description                              |
|-------------|-------------|------------------------------------------|
| 1           | 1.0.0       | Initial release                          |
| 2           | 1.0.1       | Bug fix release                          |
| 3           | 1.1.0       | Added widget support                     |
| 4           | 1.2.0       | Added export feature                     |
| 5           | 2.0.0       | Major redesign with new UI               |

### How to Update Version

1. **Edit `app/build.gradle.kts`:**

   ```kotlin
   defaultConfig {
       versionCode = 2                // Increment by 1
       versionName = "1.0.1"          // Update version string
   }
   ```

2. **Update `CHANGELOG.md`** with release notes

3. **Commit and tag:**
   ```bash
   git add app/build.gradle.kts CHANGELOG.md
   git commit -m "chore: bump version to 1.0.1"
   git tag -a v1.0.1 -m "Release v1.0.1"
   git push origin main --tags
   ```

---

## Publishing on F-Droid

[F-Droid](https://f-droid.org/) is the leading open-source Android app repository. It's completely free and emphasizes privacy and security.

### Prerequisites

- ✅ Your app is open source (hosted on GitHub, GitLab, etc.)
- ✅ App uses only FOSS (Free and Open Source Software) dependencies
- ✅ No proprietary services (Google Play Services, Firebase Analytics, etc.)
- ✅ No tracking or analytics
- ✅ Build is reproducible from source

**Year Dots meets all these requirements!** ✅

### Step 1: Prepare Your Repository

#### Add Fastlane Metadata

F-Droid uses fastlane-style metadata. Create this directory structure:

```
fastlane/
└── metadata/
    └── android/
        └── en-US/
            ├── full_description.txt
            ├── short_description.txt
            ├── title.txt
            ├── changelogs/
            │   └── 1.txt      # Changelog for versionCode 1
            │   └── 2.txt      # Changelog for versionCode 2
            └── images/
                ├── icon.png              # 512x512 app icon
                ├── featureGraphic.png    # 1024x500 banner (optional)
                └── phoneScreenshots/
                    ├── 1.png
                    ├── 2.png
                    └── 3.png
```

#### Create Metadata Files

**`fastlane/metadata/android/en-US/title.txt`:**
```
Year Dots
```

**`fastlane/metadata/android/en-US/short_description.txt`:**
```
Minimalist wallpaper showing your year as 365 dots
```

**`fastlane/metadata/android/en-US/full_description.txt`:**
```
Year Dots transforms your phone's wallpaper into a daily reminder of time's passage. 
Each day of the year is represented by a single dot in a 365-dot grid.

Features:
• 365-dot calendar grid - Visual representation of your year
• Automatic daily updates - Wallpaper refreshes at midnight
• Fully customizable colors - Choose colors for past, present, and future
• Four dot shapes - Circle, Rounded Square, Square, and Pill
• Four size options - Tiny, Small, Medium, and Large
• Live preview - See changes before applying

Privacy:
• 100% offline - No internet permission required
• No tracking or analytics
• No ads
• Open source under MIT License
```

### Step 2: Submit to F-Droid

#### Option A: Request for Packaging (RFP)

1. Go to [F-Droid RFP Issues](https://gitlab.com/fdroid/rfp/-/issues)
2. Click "New Issue"
3. Use the "Request For Packaging" template
4. Fill in:
   - **App name:** Year Dots
   - **Homepage:** https://github.com/ikrishanaa/Yeardots
   - **Source code:** https://github.com/ikrishanaa/Yeardots
   - **License:** MIT
   - **Description:** Brief description of your app
5. Submit the issue

F-Droid maintainers will review and add your app to the repository.

#### Option B: Submit a Merge Request (Faster)

If you're comfortable with Git:

1. **Fork the F-Droid Data Repository:**
   ```bash
   git clone https://gitlab.com/fdroid/fdroiddata.git
   cd fdroiddata
   ```

2. **Create metadata file** `metadata/com.krishana.onedot.yml`:
   ```yaml
   Categories:
     - Customization
     - Theming
   License: MIT
   AuthorName: Krishana
   AuthorEmail: krishanaindia773@gmail.com
   SourceCode: https://github.com/ikrishanaa/Yeardots
   IssueTracker: https://github.com/ikrishanaa/Yeardots/issues
   Changelog: https://github.com/ikrishanaa/Yeardots/blob/main/CHANGELOG.md

   AutoName: Year Dots

   RepoType: git
   Repo: https://github.com/ikrishanaa/Yeardots.git

   Builds:
     - versionName: '1.0.0'
       versionCode: 1
       commit: v1.0.0
       subdir: app
       gradle:
         - yes

   AutoUpdateMode: Version
   UpdateCheckMode: Tags
   CurrentVersion: 1.0.0
   CurrentVersionCode: 1
   ```

3. **Test locally:**
   ```bash
   fdroid lint com.krishana.onedot
   fdroid build com.krishana.onedot
   ```

4. **Submit merge request** on GitLab

### Step 3: After Acceptance

Once accepted:
- Your app will appear on F-Droid within 1-2 weeks
- F-Droid will automatically build new versions when you create tags
- Users can install/update via F-Droid app

### F-Droid Anti-Features

Ensure your app doesn't have these [anti-features](https://f-droid.org/en/docs/Anti-Features/):
- ❌ Ads
- ❌ Tracking
- ❌ Non-Free Network Services
- ❌ Non-Free Dependencies
- ❌ Promoting Non-Free Addons

**Year Dots has NONE of these!** ✅

---

## Publishing on IzzyOnDroid

[IzzyOnDroid](https://apt.izzysoft.de/fdroid/) is a faster F-Droid-compatible repository. Apps get added within days instead of weeks.

### How to Submit

1. Go to [IzzyOnDroid Application Inclusion Request](https://gitlab.com/AuroraOSS/aurorastore/-/issues)
2. Or open an issue at: https://apt.izzysoft.de/fdroid/

**Requirements:**
- Open source
- Signed APK available in GitHub Releases
- No tracking/ads

IzzyOnDroid downloads APKs directly from your GitHub Releases, so ensure you have proper release workflows set up.

---

## Publishing on GitHub Releases

Year Dots already has GitHub Releases configured! Here's how it works:

### Automatic Release (Recommended)

1. **Update version** in `app/build.gradle.kts`
2. **Update CHANGELOG.md**
3. **Create and push tag:**
   ```bash
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin v1.1.0
   ```
4. GitHub Actions automatically:
   - Builds the APK
   - Signs it (if secrets are configured)
   - Creates a GitHub Release with the APK attached

### Manual Release

1. Build APK: `./gradlew assembleRelease`
2. Go to GitHub → Releases → "Create a new release"
3. Create tag (e.g., `v1.1.0`)
4. Upload APK
5. Add release notes
6. Publish

---

## Alternative Distribution Platforms

### 1. Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) lets users track GitHub releases directly.

**No action required!** Users can add your repo URL and get automatic updates.

### 2. APKPure / APKMirror

Submit your APK to these popular APK hosting sites:
- [APKPure Submission](https://apkpure.com/developer/console.html)
- [APKMirror Upload](https://www.apkmirror.com/apk-upload/)

### 3. Amazon Appstore

Free to publish:
1. Create [Amazon Developer Account](https://developer.amazon.com/)
2. Submit APK via the Developer Console
3. Free to list

### 4. Samsung Galaxy Store

For Samsung devices:
1. Register at [Samsung Developer Portal](https://developer.samsung.com/)
2. Submit app via Seller Portal
3. No fee for registration

### 5. Huawei AppGallery

Large Chinese/international market:
1. Register at [Huawei Developer](https://developer.huawei.com/)
2. Submit via AppGallery Connect
3. No Google services dependency (good for Year Dots!)

---

## Fastlane Metadata Setup

Complete setup for F-Droid and automated metadata:

### Directory Structure

```bash
mkdir -p fastlane/metadata/android/en-US/{changelogs,images/phoneScreenshots}
```

### Create All Files

```bash
# Title
echo "Year Dots" > fastlane/metadata/android/en-US/title.txt

# Short description (max 80 chars)
echo "Minimalist wallpaper showing your year as 365 dots" > fastlane/metadata/android/en-US/short_description.txt

# Full description
cat > fastlane/metadata/android/en-US/full_description.txt << 'EOF'
Year Dots transforms your phone's wallpaper into a daily reminder of time's passage. Each day of the year is represented by a single dot in a 365-dot grid that updates automatically at midnight.

Features:
• 365-dot calendar grid - Visual representation of the entire year
• Automatic daily updates - Wallpaper refreshes at midnight
• Fully customizable colors - Choose colors for past, present, future, and background
• Four dot shapes - Circle, Rounded Square, Square, and Pill
• Four size options - Tiny, Small, Medium, and Large dot densities
• Live preview - See changes in real-time before applying

Privacy & Performance:
• 100% Offline - No internet permission, no tracking
• Battery Efficient - Optimized background tasks
• AMOLED-Friendly - Dark backgrounds save battery
• No Data Collection - Your privacy is guaranteed

Inspired by life calendar visualizations and the philosophy that awareness of time's finite nature helps us live more intentionally.

Open source under MIT License.
EOF

# Changelog for version 1 (versionCode)
cat > fastlane/metadata/android/en-US/changelogs/1.txt << 'EOF'
Initial release:
• 365-dot calendar wallpaper
• Four dot shapes: Circle, Rounded, Square, Pill
• Four size densities
• Full color customization
• Live preview
• Automatic daily updates
EOF
```

### Add Screenshots

Add PNG screenshots to `fastlane/metadata/android/en-US/images/phoneScreenshots/`:
- Name them `1.png`, `2.png`, `3.png`, etc.
- Recommended size: 1080x1920 or 1440x2560
- Show main features of the app

### Add App Icon

Add your 512x512 icon to:
```
fastlane/metadata/android/en-US/images/icon.png
```

---

## Summary Checklist

### Before First Release
- [ ] Set up Fastlane metadata structure
- [ ] Add screenshots and icon
- [ ] Configure GitHub Actions for releases
- [ ] Generate release keystore

### For Each Release
- [ ] Update `versionCode` (increment by 1)
- [ ] Update `versionName` (follow semver)
- [ ] Update `CHANGELOG.md`
- [ ] Create changelog file in `fastlane/metadata/android/en-US/changelogs/{versionCode}.txt`
- [ ] Create and push git tag
- [ ] Verify GitHub Release is created
- [ ] Submit to F-Droid (first time only)

### Platform Submissions
- [ ] F-Droid (open RFP or submit MR)
- [ ] IzzyOnDroid (automatic from GitHub)
- [ ] GitHub Releases (automatic)
- [ ] Optional: APKPure, Amazon, Samsung

---

## Need Help?

- **F-Droid Documentation:** https://f-droid.org/en/docs/
- **F-Droid Forum:** https://forum.f-droid.org/
- **IzzyOnDroid:** https://apt.izzysoft.de/fdroid/
- **Year Dots Issues:** https://github.com/ikrishanaa/Yeardots/issues

---

**Happy Publishing! 🚀**
