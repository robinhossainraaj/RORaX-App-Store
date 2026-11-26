# RORAX App Store: Complete Build Specification

> **Instructions for the AI reading this file:** This document is the single source of truth for building the app. Read all of it before writing any code. Build exactly what is described. Where a detail is missing, choose the simplest standard Android solution and add a short `// NOTE:` comment explaining the choice. Do not skip any screen, state, or edge case. Output the **complete project** (every Gradle file, the manifest, resources and Kotlin source file) so it opens and builds in Android Studio without edits.

---

## 1. What this app is

**RORAX App Store** is a personal, installable Android app store. It lists the apps I (the developer) have built, lets users see details and screenshots, download the APK, install it, update it, and open it. It works like a mini Google Play Store.

- **App name:** RORAX App Store
- **Package name / applicationId:** `com.rorax.appstore`
- **Platform:** Android only (native)
- **Language:** Kotlin
- **UI toolkit:** Jetpack Compose with Material 3
- **minSdk:** 24 (Android 7.0)
- **targetSdk / compileSdk:** 34
- **Distribution:** this store is itself sideloaded as an APK (NOT published on Google Play), so using `QUERY_ALL_PACKAGES` and `REQUEST_INSTALL_PACKAGES` is acceptable.
- **Backend:** none. All app data comes from one JSON file hosted on GitHub. APK files are hosted in GitHub Releases.
- **Screens:** Splash, Home, App Details, Installed, Settings, plus a Request APK sheet/screen and a Notifications list screen.

---

## 2. Tech stack (use exactly these)

| Purpose | Library |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Navigation | `androidx.navigation:navigation-compose` |
| Architecture | MVVM, `ViewModel`, `StateFlow`, Kotlin Coroutines |
| Networking | OkHttp (and Retrofit if wanted) |
| JSON | `kotlinx.serialization` (set `ignoreUnknownKeys = true`, `coerceInputValues = true`) |
| Images | Coil Compose (`io.coil-kt:coil-compose`) |
| Local settings | Jetpack DataStore (Preferences) |
| Background work | WorkManager |
| Splash | `androidx.core:core-splashscreen` |
| Icons | `androidx.compose.material:material-icons-extended` |
| DI | Keep simple: manual DI with an `AppContainer` in the `Application` class (no Hilt) |

Use the latest stable versions. Use a Gradle version catalog (`libs.versions.toml`) and Kotlin DSL (`build.gradle.kts`).

---

## 3. Project structure

```
com.rorax.appstore
├── RoraxApp.kt                      // Application class, AppContainer
├── MainActivity.kt                  // single activity, hosts Compose + NavHost
├── data
│   ├── model
│   │   ├── StoreResponse.kt         // root JSON model
│   │   ├── StoreInfo.kt
│   │   └── AppItem.kt
│   ├── remote
│   │   └── StoreApi.kt              // fetches JSON
│   ├── local
│   │   ├── JsonCache.kt             // saves/reads last good JSON file
│   │   └── SettingsStore.kt         // DataStore (theme, last-seen versions)
│   └── repository
│       └── StoreRepository.kt
├── domain
│   ├── InstallState.kt              // sealed class
│   └── InstallStateResolver.kt      // compares installed vs JSON
├── install
│   ├── ApkDownloader.kt             // OkHttp download with progress
│   ├── ApkInstaller.kt              // PackageInstaller session
│   ├── InstallResultReceiver.kt     // BroadcastReceiver
│   └── InstalledAppsProvider.kt     // PackageManager queries
├── worker
│   └── UpdateCheckWorker.kt         // periodic check + notification
├── notification
│   └── NotificationHelper.kt
└── ui
    ├── theme  (Color.kt, Type.kt, Shape.kt, Theme.kt)
    ├── navigation (NavGraph.kt, Routes.kt)
    ├── components (AppCard, InstallButton, ScreenshotRow, ErrorView, ...)
    ├── splash
    ├── home
    ├── details
    ├── installed
    ├── settings
    ├── request
    └── notifications
```

---

## 4. Data source and models

### 4.1 JSON URL (hardcode in one constant `Constants.JSON_URL`)

```
https://raw.githubusercontent.com/robinhossainraaj/App-Store-/refs/heads/main/JSON/apps.json
```

### 4.2 JSON structure

```json
{
  "store": {
    "developerName": "RORAX",
    "developerBio": "Non Profit Organisation",
    "telegram": "https://t.me/my_dad_raaj",
    "email": "rsrobinhossin3@gmail.com",
    "facebook": "https://facebook.com/robinisrorax"
  },
  "apps": [
    {
      "name": "Living Life",
      "package": "com.rorax.note",
      "icon": "https://raw.githubusercontent.com/robinhossainraaj/App-Store-/refs/heads/main/Icons/ic_launcher.png",
      "screenshots": ["https://.../note1.png", "https://.../note2.png"],
      "description": "Paragraph about the app.",
      "versionName": "1.0.1",
      "versionCode": 2,
      "apkUrl": "https://github.com/USER/REPO/releases/download/note-v1.0.1/note.apk",
      "sizeMB": 12.4,
      "updatedAt": "2026-11-06"
    }
  ]
}
```

### 4.3 Kotlin models

```kotlin
@Serializable
data class StoreResponse(
    val store: StoreInfo = StoreInfo(),
    val apps: List<AppItem> = emptyList()
)

@Serializable
data class StoreInfo(
    val developerName: String = "",
    val developerBio: String = "",
    val telegram: String = "",
    val email: String = "",
    val facebook: String = ""
)

@Serializable
data class AppItem(
    val name: String = "",
    @SerialName("package") val packageName: String = "",
    val icon: String = "",
    val screenshots: List<String> = emptyList(),
    val description: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val apkUrl: String = "",
    val sizeMB: Double = 0.0,
    val updatedAt: String = ""
)
```

**Rules:**
- Every field has a default so one missing field never crashes parsing.
- `package` is a Kotlin keyword, so map it with `@SerialName("package")`.
- Skip (filter out) any app whose `packageName` or `apkUrl` is blank.
- `screenshots` can be empty. Then the screenshots section is hidden entirely on the Details screen.
- Format size as `12.4 MB`. If `sizeMB` is 0, show nothing for the size.
- Format `updatedAt` for display as `06 Nov 2026`. If it can't be parsed, show the raw text.

### 4.4 Repository behaviour (`StoreRepository`)

1. On app start, and on pull-to-refresh, download the JSON with OkHttp (timeout 15 s connect, 20 s read).
2. Add header `Cache-Control: no-cache` and append `?t=<currentTimeMillis>` to the URL so GitHub's ~5 minute CDN cache doesn't return stale data on manual refresh.
3. On success: parse, save the raw JSON string to `filesDir/store_cache.json`, and emit `Success`.
4. On failure (no internet, timeout, bad JSON): if a cached file exists, parse it and emit `Success` with a flag `isOffline = true` (show a small "Offline, showing saved data" banner). If no cache exists, emit `Error` with a friendly message and a **Retry** button.
5. Expose a `StateFlow<StoreUiState>` where:

```kotlin
sealed interface StoreUiState {
    data object Loading : StoreUiState
    data class Success(val store: StoreInfo, val apps: List<AppItem>, val isOffline: Boolean) : StoreUiState
    data class Error(val message: String) : StoreUiState
}
```

---

## 5. Install state logic (the core feature)

### 5.1 States

```kotlin
sealed interface InstallState {
    data object NotInstalled : InstallState                       // button: INSTALL
    data class UpdateAvailable(val installedCode: Long) : InstallState   // button: UPDATE
    data object UpToDate : InstallState                           // button: OPEN
    data class Downloading(val progress: Int) : InstallState      // 0..100, button: progress + Cancel
    data object Installing : InstallState                         // spinner, "Installing..."
}
```

### 5.2 Resolving the state

For every app in the JSON:

```kotlin
val installedCode: Long? = try {
    val info = packageManager.getPackageInfo(app.packageName, 0)
    if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
} catch (e: PackageManager.NameNotFoundException) { null }

state = when {
    installedCode == null -> NotInstalled
    installedCode < app.versionCode -> UpdateAvailable(installedCode)
    else -> UpToDate
}
```

**Critical:** always compare the integer `versionCode`, never the `versionName` text. (Comparing "1.0.10" and "1.0.9" as text gives wrong results.)

### 5.3 When to re-resolve

- When the JSON finishes loading.
- When the app returns to the foreground (`Lifecycle.Event.ON_RESUME`).
- After an install result is received.
- On pull-to-refresh.
- When the package is added/removed/replaced (optional: register a `BroadcastReceiver` for `ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, `ACTION_PACKAGE_REPLACED`).

### 5.4 Buttons

| State | Button label | Colour | Action |
|---|---|---|---|
| NotInstalled | INSTALL | primary | download then install |
| UpdateAvailable | UPDATE | primary | download then install (the Android installer upgrades in place) |
| UpToDate | OPEN | secondary / outlined | launch the app using `packageManager.getLaunchIntentForPackage` |
| Downloading | progress bar with `45%` text and a CANCEL button | | cancel the download, delete the partial file |
| Installing | spinner with "Installing..." | | none |

If `getLaunchIntentForPackage` returns null (an app with no launcher activity), show a Toast: "This app can't be opened directly."

---

## 6. Download and install flow

### 6.1 Download (`ApkDownloader`)

- Use OkHttp with streaming. Follow redirects (GitHub release URLs redirect).
- Save to `context.getExternalFilesDir("apks")/<package>-<versionCode>.apk`. Create the folder if missing.
- Report progress (0 to 100) through a `Flow<DownloadEvent>` (`Progress(percent)`, `Done(file)`, `Failed(reason)`).
- If the server doesn't send `Content-Length`, show an indeterminate progress bar.
- Support cancel: cancel the coroutine job, close the stream, delete the partial file.
- If a complete file for the same package + versionCode already exists, skip the download and go straight to install.
- Run downloads in a coroutine that survives screen changes (hold the job in an application-scoped `CoroutineScope` inside `AppContainer`, with a `Map<String, InstallState>` exposed as `StateFlow`). Leaving the Details screen must NOT cancel the download.

### 6.2 Pre-install permission check

```kotlin
if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
    // show a dialog: "To install apps, allow RORAX App Store to install unknown apps."
    // button opens Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES with Uri.parse("package:$packageName")
    // when the user returns (ON_RESUME) and permission is granted, continue the pending install automatically.
}
```

### 6.3 Install (`ApkInstaller`)

Use the `PackageInstaller` session API:

1. `val installer = packageManager.packageInstaller`
2. Create `SessionParams(MODE_FULL_INSTALL)`; call `setAppPackageName(app.packageName)`; on API 31+ call `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` (this only makes updates silent when allowed by the system; otherwise the user confirms).
3. Open the session, write the APK bytes via `session.openWrite("apk", 0, file.length())`, call `session.fsync(out)`, close.
4. Commit with an `IntentSender` from a `PendingIntent.getBroadcast(...)` targeting `InstallResultReceiver` (use `FLAG_MUTABLE` on API 31+, because the system needs to add extras).
5. In `InstallResultReceiver.onReceive`, read `PackageInstaller.EXTRA_STATUS`:
   - `STATUS_PENDING_USER_ACTION` -> get `Intent.EXTRA_INTENT` (the confirm screen) and `startActivity` it with `FLAG_ACTIVITY_NEW_TASK`.
   - `STATUS_SUCCESS` -> update state, re-resolve install states, delete the APK file, show a Toast "Installed successfully".
   - Any failure status -> show a clear message mapped from the status (`STATUS_FAILURE_CONFLICT`: "Signature mismatch. Uninstall the old version first.", `STATUS_FAILURE_INCOMPATIBLE`: "This app is not compatible with your device.", `STATUS_FAILURE_STORAGE`: "Not enough storage.", otherwise: the `EXTRA_STATUS_MESSAGE`).
6. Also handle the case where the system installer is cancelled by the user (`STATUS_FAILURE_ABORTED`): go back to the previous state silently, no error.

**Important note for the developer (put as a comment):** updates only work when the new APK is signed with the **same keystore** as the installed one.

### 6.4 FileProvider

Even though PackageInstaller reads the file directly, declare a `FileProvider` (`authorities = "${applicationId}.fileprovider"`) with `res/xml/file_paths.xml` exposing `external-files-path name="apks" path="apks/"` as a fallback for a plain `ACTION_VIEW` install intent.

---

## 7. AndroidManifest.xml requirements

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- Single launcher activity `MainActivity` with the splash theme.
- `InstallResultReceiver` registered (`exported="false"`).
- `FileProvider` registered (`exported="false"`, `grantUriPermissions="true"`).
- `android:usesCleartextTraffic="false"` (all URLs are HTTPS).
- App label: `RORAX App Store`.
- Launcher icon: placeholder for now (the developer will replace it). Provide an adaptive icon setup with a placeholder foreground drawable named `ic_logo` so replacing one file swaps the logo everywhere (splash, launcher, in-app).
- Request the `POST_NOTIFICATIONS` runtime permission on Android 13+ the first time the Home screen opens (with a short explanation dialog first). If denied, the app keeps working and simply doesn't send notifications.

---

## 8. Theme and design system

> **NOTE FOR THE DEVELOPER (not for the AI):** The values in this section are my DEFAULT proposal. They are not copied from earlier RORAX apps. Replace the colour tokens in 8.1 with your real brand colours before building. Everything in the app must read colours ONLY from these tokens, so changing them here changes the whole app.

### 8.1 Colour tokens (put in `Color.kt`, every colour is a named constant)

**Dark theme (the default look)**

| Token | Hex | Used for |
|---|---|---|
| `Background` | `#0B0F14` | screen background |
| `Surface` | `#131A22` | cards, top bar, bottom nav |
| `SurfaceVariant` | `#1B2430` | search field, chips, progress track |
| `Primary` | `#00E5A8` | main buttons, active nav icon, accents |
| `OnPrimary` | `#00382A` | text/icons on primary |
| `Secondary` | `#4DA3FF` | OPEN button outline, links |
| `Error` | `#FF5C6C` | error text, failure states |
| `OnBackground` | `#E8EEF4` | primary text |
| `OnSurfaceMuted` | `#8A98A8` | secondary text (package name, size) |
| `Outline` | `#2A3644` | borders, dividers |

**Light theme**

| Token | Hex |
|---|---|
| `Background` | `#F5F8FB` |
| `Surface` | `#FFFFFF` |
| `SurfaceVariant` | `#E8EEF4` |
| `Primary` | `#00A87E` |
| `OnPrimary` | `#FFFFFF` |
| `Secondary` | `#1F6FD1` |
| `Error` | `#D8283C` |
| `OnBackground` | `#0B0F14` |
| `OnSurfaceMuted` | `#5B6978` |
| `Outline` | `#D2DAE3` |

Map these into a Material 3 `ColorScheme` for both `darkColorScheme` and `lightColorScheme`. **Disable dynamic (Material You) colour**, so the brand colours always apply.

### 8.2 Shapes (`Shape.kt`)

- Cards: rounded 16 dp
- Buttons: rounded 12 dp
- App icons: rounded 18 dp (squircle look)
- Screenshots: rounded 12 dp
- Search field: fully rounded (pill)

### 8.3 Spacing scale

4, 8, 12, 16, 24, 32 dp. Screen horizontal padding: 16 dp. Gap between grid cards: 12 dp.

### 8.4 Status bar and navigation bar

Edge-to-edge. Status bar and navigation bar are transparent, with icon colours switching by theme.

---

## 9. Typography: Rajdhani

**Font: Rajdhani, Bold (700) and SemiBold (600) ONLY.** Do not use any other weight or any other font family anywhere in the app. Rajdhani has no italic. Never apply italic style.

### 9.1 Setup

- Download `Rajdhani-Bold.ttf` and `Rajdhani-SemiBold.ttf` from Google Fonts (open licence).
- Place them in `app/src/main/res/font/` named exactly `rajdhani_bold.ttf` and `rajdhani_semibold.ttf` (lowercase, underscores).
- Define in `Type.kt`:

```kotlin
val Rajdhani = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)
```

### 9.2 Type scale

| Style | Weight | Size | Line height | Used for |
|---|---|---|---|---|
| `displayLarge` | Bold | 32 sp | 38 sp | splash app name |
| `headlineMedium` | Bold | 24 sp | 30 sp | screen titles, details app name |
| `titleLarge` | Bold | 20 sp | 26 sp | top bar title, section titles |
| `titleMedium` | Bold | 17 sp | 22 sp | app name on cards |
| `bodyLarge` | SemiBold | 16 sp | 22 sp | description text |
| `bodyMedium` | SemiBold | 14 sp | 19 sp | package name, size, settings text |
| `labelLarge` | Bold | 15 sp | 20 sp | button text (UPPERCASE, letter spacing 0.8 sp) |
| `labelMedium` | SemiBold | 12 sp | 16 sp | small captions, chips |

Every `Text` in the app must take its style from `MaterialTheme.typography`. Never hardcode font family or size in a composable. Rajdhani runs a bit small for its size, so keep body text at 14 sp or above.

---

## 10. Navigation

- Single `NavHost` in `MainActivity`.
- **Routes:** `splash`, `home`, `installed`, `settings`, `details/{package}`, `request`, `notifications`.
- **Bottom navigation bar** (visible only on `home`, `installed`, `settings`): three items with icons and labels: **Home** (`Icons.Rounded.Home`), **Installed** (`Icons.Rounded.CheckCircle`), **Settings** (`Icons.Rounded.Settings`). The selected item uses the `Primary` colour with a pill indicator. Hide the bottom bar on Splash, Details, Request and Notifications.
- Splash navigates to Home and removes itself from the back stack (back button never returns to Splash).
- Bottom nav tabs keep their own state (`saveState = true`, `restoreState = true`, `launchSingleTop = true`).
- Screen transitions: fade + slight slide (200 ms).
- One shared `StoreViewModel` (scoped to the activity) holds the JSON data and install states so all screens stay in sync.

---

## 11. Screen specifications

### 11.1 Splash screen

- Background: `Background` colour.
- Centre: the app logo (`ic_logo`), 112 dp.
- **Animation sequence (about 1.6 s total), like Telegram:**
  1. 0 to 500 ms: logo scales from 0.6 to 1.0 with an overshoot easing and fades from 0 to 1.
  2. 400 to 900 ms: the app name **"RORAX"** (Bold, `displayLarge`, letter spacing 4 sp, `OnBackground`) fades in and slides up 16 dp, under the logo.
  3. 700 to 1100 ms: a small subtitle **"App Store"** (SemiBold, `bodyMedium`, `Primary` colour) fades in.
  4. Hold until 1600 ms, then navigate to Home. During the splash, load the JSON in the background so Home usually opens with data ready.
- Use the `androidx.core:core-splashscreen` API so the Android 12+ system splash shows only the logo on the same `Background` colour, with no second splash. Keep the system splash brief, and run the Compose animation as the Splash route.

### 11.2 Home screen

**Top app bar** (`CenterAlignedTopAppBar` or a custom row):
- Left: the title **"RORAX App Store"** (Bold, `titleLarge`).
- Right: a **Search** icon and a **Notifications** icon.
- Notifications icon shows a small `Primary` dot badge when there are unread notifications.

**Search behaviour:**
- Tapping the search icon turns the top bar into a search field (pill shape, auto-focus, a back arrow to close, a clear "X" when text is present).
- Filters the grid live (case-insensitive) by app name OR package name.
- If nothing matches, show an empty state: a search icon and **"No apps found for '<query>'"**.

**Content:**
- Grid: `LazyVerticalGrid` with `GridCells.Fixed(2)`, 12 dp gaps, 16 dp padding.
- **Pull-to-refresh** reloads the JSON (use `PullToRefreshBox`).
- A thin banner at the top when offline: "Offline, showing saved data".
- If any updates are available, a horizontal chip row at the very top: **"X updates available"** (tapping it opens the Installed tab).

**App card** (each grid item, rounded 16 dp, `Surface` colour, 1 dp `Outline` border):
- App icon (72 dp, rounded 18 dp, loaded with Coil, with a shimmer or a default placeholder while loading, and the default app icon if it fails).
- **App name** (Bold `titleMedium`, max 1 line, ellipsis).
- **Package name** (SemiBold `bodyMedium`, `OnSurfaceMuted`, max 1 line, ellipsis).
- **Size** (`12.4 MB`, SemiBold `labelMedium`, `OnSurfaceMuted`).
- A small status chip at the bottom: **INSTALL** / **UPDATE** / **OPEN** (colour by state, taps do NOT start a download from the card, they open the Details screen).
- Entire card is clickable and opens `details/{package}`.

**States:**
- `Loading`: show 6 shimmer skeleton cards.
- `Error` with no cache: centred icon, message, **Retry** button.
- `Success` with zero apps: "No apps yet. Check back soon."

### 11.3 App Details screen (Play Store style)

Scrollable column, top bar with a back arrow only (transparent, no title), no bottom bar.

1. **Header:** large icon (96 dp, rounded 22 dp) on the left. To the right: app name (Bold `headlineMedium`), developer name from `store.developerName` (SemiBold, `Primary` colour), and the package name (`OnSurfaceMuted`).
2. **Info row** (three equal columns with thin vertical dividers): **Version** (`1.0.1`), **Size** (`12.4 MB`), **Updated** (`06 Nov 2026`). Each shows a label in `labelMedium` muted and the value in Bold `titleMedium`.
3. **Main action button** (full width, 52 dp height, 12 dp radius): INSTALL / UPDATE / OPEN, or the download progress UI. While downloading: a progress bar with `Downloading... 45%`, and a small CANCEL text button beside it. While installing: `Installing...` with a spinner.
   - If the app is installed, also show the installed version below the button: `Installed version: 1.0.0`. In the update case, show `Update available: 1.0.0 to 1.0.1`.
4. **Screenshots** (only if the list is non-empty): section title **"Screenshots"**, then a horizontally scrolling `LazyRow` of images (height 280 dp, rounded 12 dp, 8 dp gap). Tapping one opens a full-screen viewer with a horizontal pager (swipe between screenshots, pinch to zoom, back to close).
5. **About this app:** section title and the description. Show 4 lines with a **"Read more"** toggle if longer.
6. **App information card:** rows for Package name, Version name, Version code, Size, Last updated.
7. **Developer section:** developer name and bio from the JSON, and the contact buttons (Telegram, Email, Facebook) as small icon chips.

If the package is not found in the loaded list (for example a stale link), show a "App not found" message with a back button.

### 11.4 Installed screen

Title: **"Installed"** (top bar). Shows ONLY apps from the store JSON whose package name is found installed on this device (matched by exact package name, using the logic in section 5).

**Layout (vertical list, not a grid):**
- Section **"Updates available (N)"** at the top, shown only if N > 0. Contains a row for each app in the `UpdateAvailable` state, plus an **"Update all"** button that downloads and installs them one at a time in a queue.
- Section **"Up to date (N)"** with the apps in `UpToDate` state.
- Each row: icon (56 dp), app name (Bold), `Installed: 1.0.0` and, when updating, `New: 1.0.1` (SemiBold muted), and a trailing button: **UPDATE** or **OPEN**. Tapping the row opens the Details screen.
- Rows show the same live download/install progress as the Details screen.

**Empty state:** "None of your store apps are installed yet." with a button to go to Home.

Example of the matching rule: the device has `com.ex.ig` v1 and the store has `com.ex.ig` v2, so the app is listed under "Updates available" with an UPDATE button. If the device has v2, it is listed under "Up to date" with an OPEN button.

### 11.5 Settings screen

Title: **"Settings"**. A scrollable column of grouped cards:

1. **Developer info card:** developer name, bio, and Telegram / Email / Facebook buttons that open the links (Telegram and Facebook with `ACTION_VIEW`, email with `ACTION_SENDTO` `mailto:`). Data comes from the JSON `store` block. Show defaults if the JSON hasn't loaded.
2. **Theme card:** a three-option selector (radio rows or segmented control): **System default**, **Light**, **Dark**. The choice is saved in DataStore and applies instantly across the whole app. Default is **Dark**.
3. **Request an APK card:** text "Want an app that's not here? Ask me to build it." and a button **"Request an APK"** that opens the Request screen.
4. **About card:** app name, version (read from `BuildConfig`/`PackageInfo`), package name `com.rorax.appstore`.
5. **Notifications toggle:** a switch to turn background update notifications on or off (saved in DataStore, default on). Turning it off cancels the WorkManager job; turning it on re-enqueues it.

### 11.6 Request APK screen

A form with:
- **App name** (required, single line)
- **What should the app do?** (multi-line, required, min 10 characters)
- **Your name** (optional)
- **Your contact** (optional)

Inline validation errors under each field.

Three send buttons in a row (full width each, icon + label): **Telegram**, **Email**, **Facebook**. All of them first build this message:

```
New APK request - RORAX App Store

App name: <name>
Details: <details>
From: <name or "Anonymous">
Contact: <contact or "Not provided">
```

- **Email:** open `ACTION_SENDTO` with `mailto:<store.email>`, subject `APK Request: <app name>`, and the message as the body (URL-encoded properly).
- **Telegram:** Telegram links cannot be pre-filled with text for a personal chat, so **copy the message to the clipboard**, show a Toast "Message copied, paste it in the chat", then open `store.telegram` with `ACTION_VIEW`.
- **Facebook:** same approach as Telegram: copy to clipboard, show the Toast, then open `store.facebook`.
- If the target app isn't installed, the `ACTION_VIEW` opens in the browser, which is fine. Catch `ActivityNotFoundException` and show a Toast.
- If `store.telegram` / `email` / `facebook` is blank in the JSON, hide that button.

### 11.7 Notifications screen

Opened from the bell icon on Home. A list of recent store events, newest first (stored locally in DataStore or a small JSON file, max 50 entries):
- "New app: <name>"
- "Update available: <name> 1.0.0 to 1.0.1"

Each row has an icon, the text, and the time ("2 h ago"). Tapping opens that app's Details screen. Opening this screen clears the unread dot. Empty state: "No notifications yet."

---

## 12. Background update check and notifications

- `UpdateCheckWorker` (a `CoroutineWorker`) runs as a **periodic work request every 6 hours**, with the constraint `NetworkType.CONNECTED`, enqueued as unique periodic work (`ExistingPeriodicWorkPolicy.KEEP`) on app start.
- It fetches the JSON (same repository logic), then compares with:
  1. The installed apps: any `UpdateAvailable` app is a candidate.
  2. A saved set of package names already seen (in DataStore): any app not in the set is a "new app".
- It sends one notification per new item (group them into one summary if there are more than 3), but **never notifies twice for the same package + versionCode** (store `lastNotifiedVersionCode` per package in DataStore).
- Notification channel: id `store_updates`, name "App updates", importance default.
- Tapping a notification opens the app on that app's Details screen (use a deep link or an intent extra).
- The first ever run must NOT notify for every existing app. On the first run, just record the current apps as "seen".
- Skip everything if the notification setting is off or the permission is denied.

---

## 13. Settings storage (DataStore keys)

| Key | Type | Default |
|---|---|---|
| `theme_mode` | String ("system" / "light" / "dark") | "dark" |
| `notifications_enabled` | Boolean | true |
| `seen_packages` | Set<String> | empty |
| `last_notified_<package>` | Long | 0 |
| `notification_history` | String (JSON list) | "[]" |
| `first_run_done` | Boolean | false |

---

## 14. Error handling and edge cases (all must be handled)

- No internet on first launch with no cache: friendly error screen + Retry.
- JSON malformed: treat as a failed fetch and fall back to the cache.
- Apps with a blank `package` or `apkUrl`: silently skipped.
- Icon or screenshot failing to load: default drawable, never a crash or a blank hole.
- Download interrupted (connection lost): show "Download failed. Tap to retry." and keep the state `NotInstalled` / `UpdateAvailable`.
- Not enough storage: catch `IOException` and show the message.
- User denies the install permission: show the dialog again the next time they tap Install, never loop.
- Installing the same package twice in parallel is blocked (disable the button while Downloading / Installing).
- Screen rotation, process death and back navigation must not lose or restart a running download.
- Very long app names or descriptions are truncated or scrollable and never break the layout.
- Phones with small screens (320 dp wide) and tablets: the grid stays 2 columns on phones and uses `GridCells.Adaptive(160.dp)` on wide screens (600 dp and above).
- Accessibility: every icon has a `contentDescription`, and tap targets are at least 48 dp.
- Use `R.string` resources for all user-facing text so it can be translated later.

---

## 15. Things NOT to do

- Do not use Hilt, Room, Firebase, or any backend.
- Do not use dynamic colour.
- Do not use any font other than Rajdhani Bold / SemiBold.
- Do not compare version names as text. Use `versionCode` only.
- Do not request the `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` permissions (not needed).
- Do not hardcode colours, font sizes or strings inside screens.
- Do not leave `TODO()` calls or half-implemented screens.
- Do not use deprecated APIs where a current one exists (for example, `AsyncTask`, `startActivityForResult`).

---

## 16. Acceptance checklist (the app is done when all of these work)

- [ ] The project opens in Android Studio and builds a debug APK with no errors.
- [ ] Splash animation plays once, then Home opens, with no double splash on Android 12+.
- [ ] Home loads the apps from the GitHub JSON in a 2-column grid with icon, name, package and size.
- [ ] Search filters by name and package.
- [ ] Pull-to-refresh works, and the offline banner shows when there is no internet but a cache exists.
- [ ] Tapping a card opens a Play Store-style Details screen with screenshots (hidden if none).
- [ ] INSTALL downloads with a progress bar, then the system installer appears and installs the APK.
- [ ] An installed app with a lower versionCode shows UPDATE, with an equal versionCode it shows OPEN, and OPEN launches it.
- [ ] The Installed screen lists matching apps, with an "Updates available" section on top.
- [ ] Settings shows developer info, the theme switch (Light / Dark / System) works instantly and persists.
- [ ] The Request screen sends via Email, Telegram (clipboard + open) and Facebook (clipboard + open).
- [ ] The background worker notifies about new apps and updates without duplicates.
- [ ] All text uses Rajdhani Bold or SemiBold.
- [ ] No crash on rotation, offline mode, a broken icon URL, or a denied permission.

---

## 17. Files the developer will supply or swap later

- **Logo:** replace `res/drawable/ic_logo.xml` (or `.png`) and the adaptive launcher icon files.
- **Fonts:** `rajdhani_bold.ttf` and `rajdhani_semibold.ttf` in `res/font/`.
- **Colours:** edit only `Color.kt` to re-theme the whole app.
- **JSON:** edited on GitHub. No app rebuild needed to add or change apps.
