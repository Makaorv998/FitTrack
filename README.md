# FitTrack — Smart Auto Update & Changelog System

FitTrack is an Android (Kotlin + Jetpack Compose + Firebase) fitness application featuring an integrated **Smart Auto Update & Changelog System** supporting both **Google Play In-App Updates** (Flexible & Immediate) and **Verified Direct APK Updates** (SHA-256 + signing certificate verification).

---

## Architecture Overview

- **Update Detection**:
  - `InstalledAppInfoProvider` reads installed `versionCode`, `versionName`, distribution channel (`com.android.vending` vs. direct APK), signing certificate SHA-256 fingerprints, and OS permissions.
  - `AppUpdateRepository` queries public Firestore release metadata (`/app_releases`) and policy config (`/app_config/update_policy`).
  - `AppUpdateEngine` evaluates semantic versions, severity (`optional`, `recommended`, `critical`, or forced via `minSupportedVersionCode`), check-interval throttling, first-install vs. upgrade state, and duplicate notification suppression.
  - `UpdatePreferencesStore` (DataStore) caches releases, check timestamps, last seen version, shown What's New version, read release IDs, and notification preferences.
  - `MainActivity` observes lifecycle `ON_RESUME` to check updates automatically within the configured interval and handles `POST_NOTIFICATIONS`, Google Play `ActivityResultLauncher`, Unknown Sources settings, and notification tap deep-links.

- **Update Distribution**:
  - **Google Play**: `PlayInAppUpdateManager` wraps `AppUpdateManager` for Flexible background downloads + restart completion and Immediate updates for high-priority/critical releases, plus external Play Store URL fallback if needed.
  - **Direct APK**: `ApkUpdateInstaller` enforces HTTPS + `trustedApkHosts` allowlist, streams and hashes the APK (`apkSha256`), validates package ID, blocks version downgrades, verifies signing certificates against both the installed app and `expectedSigningCertSha256`, checks `REQUEST_INSTALL_PACKAGES`, and opens Android's system package installer via `FileProvider`.

- **UI & First-Open Experience**:
  - `UpdateCenterScreen`: Installed version, manual Check for Updates, offline/server error banners, Play & Direct APK install controls, and notification/interval settings.
  - `WhatsNewScreen`: Latest and previous categorized release notes (`newFeatures`, `improvements`, `bugFixes`, `securityChanges`), localized entries, read/unread badges, and auto-presentation once per upgrade.
  - Integrated into `HomeScreen` (`UpdateAvailabilityBanner`), `SettingsScreen` (`App Updates & Changelog`), and `FitTrackRoutes` (`UpdateCenter`, `WhatsNew`).

---

## Configuration Requirements

1. **AndroidManifest & Resources**:
   - `app/src/main/AndroidManifest.xml` declares `POST_NOTIFICATIONS`, `REQUEST_INSTALL_PACKAGES`, and `androidx.core.content.FileProvider` (`${applicationId}.fileprovider` mapped to `cache-path: updates/` via `res/xml/file_paths.xml`).
   - `app/src/main/res/values/firebase_applet_config.xml` configures `firestore_database_id`.
   - `app/build.gradle.kts` increments `versionCode` and `versionName` for each release and includes `com.google.android.play:app-update` + `app-update-ktx`.

2. **Firebase Firestore Collections**:
   - `/app_config/update_policy` (publicly `get`-readable, admin-only write)
   - `/app_releases/{releaseId}` (publicly readable when `isPublished == true`, admin-only write)
   - `/admins/{uid}` (admin directory; write-locked in `firestore.rules`, provisioned via Firebase Console or Admin SDK)

> **Security note**: Never store release keystore files, keystore passwords, or private signing keys in Firestore or client code. Only store the **public** SHA-256 certificate digest (`expectedSigningCertSha256`) and APK checksum (`apkSha256`).

---

## Admin Release Publishing Workflow

Because FitTrack has no separate in-app admin portal, administrators publish releases directly via the **Firebase Console** or an authorized **Firebase Admin SDK** script.

### Step 1: Grant Admin Access (One-Time)
Either sign in with a bootstrapped verified admin email defined in `firestore.rules`, **or** create `/admins/{uid}` in the Firebase Console / Admin SDK:
```json
{
  "email": "admin@yourdomain.com",
  "role": "release_admin"
}
```

### Step 2: Build & Compute APK Verification Digests (For Direct APK Releases)
1. Increment `versionCode` and `versionName` in `app/build.gradle.kts` and build the signed release APK (`gradle assembleRelease`).
2. Compute the APK file SHA-256 digest:
   ```bash
   sha256sum app/build/outputs/apk/release/app-release.apk
   ```
3. Extract the **public** signing certificate SHA-256 fingerprint:
   ```bash
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```
   (Copy the 64-character hex SHA-256 digest.)
4. Upload the signed APK to your HTTPS release host (matching `trustedApkHosts` in `/app_config/update_policy`).

### Step 3: Publish `/app_config/update_policy`
Create or update document `/app_config/update_policy`:
```json
{
  "checkIntervalMinutes": 360,
  "minSupportedVersionCode": 1,
  "latestVersionCode": 2,
  "trustedApkHosts": ["downloads.yourdomain.com"],
  "defaultDistributionChannel": "both",
  "playUpdatePriorityThreshold": 4,
  "updatedAt": "<SERVER_TIMESTAMP>"
}
```

### Step 4: Publish `/app_releases/v2` (Document ID matches `^[a-zA-Z0-9_-]+$`)
Create the release document with **every required field** matching `firestore.rules`:
```json
{
  "versionName": "1.2.0",
  "versionCode": 2,
  "releaseDate": "2026-11-15",
  "title": "Smart Recovery & Update Center",
  "summary": "Adds automated update checks, What's New history, and performance fixes.",
  "severity": "optional",
  "minSupportedVersionCode": 1,
  "minSupportedVersionName": "1.0",
  "distributionChannel": "both",
  "apkDownloadUrl": "https://downloads.yourdomain.com/fittrack-1.2.0.apk",
  "apkSha256": "<64-CHAR-HEX-APK-SHA256>",
  "expectedSigningCertSha256": "<64-CHAR-HEX-CERT-SHA256>",
  "playStoreUrl": "https://play.google.com/store/apps/details?id=com.aistudio.fittrack.vrmqtx",
  "newFeatures": [
    "Smart Update Center in Settings",
    "What's New changelog and version history"
  ],
  "improvements": [
    "Faster workout analytics rendering"
  ],
  "bugFixes": [
    "Fixed rest timer pause synchronization"
  ],
  "securityChanges": [
    "Enforced HTTPS, SHA-256, and signing certificate checks on APK updates"
  ],
  "localizedEntries": {
    "es": {
      "title": "Centro de Actualizaciones",
      "summary": "Detección automática de actualizaciones e historial de cambios.",
      "newFeatures": ["Centro de Actualizaciones en Ajustes"],
      "improvements": [],
      "bugFixes": [],
      "securityChanges": []
    }
  },
  "isPublished": true,
  "createdAt": "<SERVER_TIMESTAMP>",
  "updatedAt": "<SERVER_TIMESTAMP>"
}
```
- Set `"severity": "critical"` (and optionally raise `"minSupportedVersionCode"`) for mandatory security or compatibility updates.
- For Google Play releases, upload the signed AAB in Play Console and set the Play In-App Update priority (`0`–`5`; `>= playUpdatePriorityThreshold` triggers Immediate update prompts).

---

## Testing & Verification

Run unit, Robolectric, and Compose UI tests:
```bash
gradle testDebugUnitTest --no-daemon
```
Run Firestore rules tests (with Firebase Emulator Suite):
```bash
firebase emulators:exec --only firestore "node --test firestore.test.js"
```
