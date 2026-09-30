# London Underground Car Number Log

A pocket logbook for tube spotters. Type the number stencilled on the end of a
London Underground car and it records the sighting with a timestamp and your
GPS position, matches it against known rolling stock, and keeps a searchable
history.

The primary app (`android-native/`) is a native Kotlin + Jetpack Compose
Android app backed by Cloud Firestore, so your log survives an app data
clear, and — if you link a Google account in Settings — a reinstall or a
switch to a new phone too. An earlier Capacitor/WebView build
(`android-app/`) is kept for reference but is no longer what's released.

## Features

- **Three input styles** — Keypad (large keys + hints), Ledger (today's log as
  a running list), or Plate (big single-number display) — switchable in
  Settings.
- **Stock recognition** — matches the number you type against known London
  Underground rolling stock ranges (1972/1973/1992/1995/1996/2009 Stock, S
  Stock) and shows the line(s) it runs on.
- **Location tagging** — each sighting is stamped with GPS coordinates and, if
  you're near one, the closest station.
- **History** — every sighting, grouped by day, searchable by car number, with
  a per-car detail view showing every time you've seen it.
- **CSV export** — share your full log as a CSV file via the system share sheet.
- **Cloud-backed, offline-first** — sightings are stored in Cloud Firestore
  with offline persistence: logging works with no signal, and syncs once
  back online. See [Firebase setup](#firebase-setup) before your first build.
- **Google Sign-In (optional)** — Settings → Account lets you link the
  anonymous account to a Google sign-in, so your log survives a reinstall or
  a switch to a new phone, without losing any existing data.
- **Update check** — Settings shows the installed version and a "Check for
  updates" button that looks at this repo's latest GitHub Release; if it's
  newer, it downloads and installs the APK natively. See
  [Releasing an update](#releasing-an-update).

## Repository layout

```
android-native/              Primary app: native Kotlin + Jetpack Compose
  app/google-services.json.example   Template — see Firebase setup
  app/google-services.json           Your real config goes here (gitignored)
  app/src/main/java/com/keithstack/carlog/
    data/                     Stock lookup, Sighting model, Firestore/DataStore repos
    location/                 LocationManager wrapper
    update/                   GitHub-release update checker + native installer
    ui/                       ViewModel, screens, theme
CarLogNative.apk              Prebuilt debug APK of the native app

Car Log App.html             Original self-contained web app (superseded)
android-app/                  Legacy Capacitor/WebView project (superseded)
CarLog.apk                    Prebuilt debug APK of the legacy web app
```

## Firebase setup

The native app uses Cloud Firestore (anonymous auth, upgradeable to Google
Sign-In) for the sightings log. To connect it to your own Firebase project:

1. In the [Firebase console](https://console.firebase.google.com), create a
   project (or use an existing one).
2. Add an Android app with package name `com.keithstack.carlog`.
3. Enable **Cloud Firestore** (Build → Firestore Database), and publish
   security rules scoping each user to their own data:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```
4. Enable **Anonymous** sign-in (Build → Authentication → Sign-in method).
5. To let users back up their log with **Google Sign-In** (Settings → Account
   → "Sign in with Google" links the existing anonymous account in place, so
   no data is lost): also enable the **Google** sign-in provider, and add
   this build's SHA-1 fingerprint under Project settings → Your apps →
   Add fingerprint. For the debug keystore:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
6. Download the generated `google-services.json` and place it at
   `android-native/app/google-services.json`, replacing the placeholder.

Without a real `google-services.json`, the app still builds and runs —
Firestore's local cache means logging still works — but nothing syncs to a
real backend until you add your own project's config. Without Google
Sign-In configured, the app still works fully on anonymous auth alone; the
Settings screen just won't complete the linking flow.

## Installing on Android

Grab [`CarLogNative.apk`](CarLogNative.apk) and either:

- Copy it to your phone (USB, email, cloud drive, etc.) and open it — you'll
  be prompted to allow installing from that source, or
- With the phone connected over USB with debugging enabled:
  ```
  adb install CarLogNative.apk
  ```

This is a debug build signed with a debug key, which is fine for installing
on your own device but not for distribution through the Play Store.

On first launch, allow the location permission prompt so sightings get GPS
coordinates — the app works without it, sightings will just be recorded
without a location.

## Building from source

Requirements:

- A JDK compatible with the Android Gradle Plugin in use (JDK 21 is known to
  work; very new JDKs may fail with `Unsupported class file major version`)
- Android SDK (platform 36, build-tools, platform-tools), with
  `ANDROID_HOME`/`ANDROID_SDK_ROOT` set and licenses accepted
- Your own `android-native/app/google-services.json` (see
  [Firebase setup](#firebase-setup)) — the placeholder checked in lets it
  build and run against Firestore's local cache only

```
cd android-native
./gradlew assembleDebug
```

The APK is written to
`android-native/app/build/outputs/apk/debug/app-debug.apk`.

## Releasing an update

The in-app update check compares its own `APP_VERSION` constant (in
`android-native/app/src/main/java/com/keithstack/carlog/ui/CarLogViewModel.kt`)
against the `tag_name` of this repo's
[latest GitHub Release](../../releases/latest), and offers whichever asset in
that release matches `*.apk`. To ship an update:

1. Bump `APP_VERSION` in `CarLogViewModel.kt`.
2. Bump `versionCode`/`versionName` in `android-native/app/build.gradle` to
   match.
3. Rebuild (`./gradlew assembleDebug`) and copy the resulting APK to
   `CarLogNative.apk`.
4. Commit, push, then tag a new GitHub Release named `vX.Y` (matching
   `APP_VERSION`) with `CarLogNative.apk` attached — e.g.
   `gh release create vX.Y CarLogNative.apk`.

Version comparison is numeric per dot-separated segment (`1.10` > `1.9`), and
the `v` prefix on the tag is ignored.

## Permissions

| Permission                 | Why                                              |
|-----------------------------|---------------------------------------------------|
| `ACCESS_FINE_LOCATION`      | Tag sightings with GPS coordinates                |
| `ACCESS_COARSE_LOCATION`    | Fallback location accuracy                        |
| `VIBRATE`                   | Haptic feedback on keypad taps                    |
| `INTERNET`                  | Firestore sync and the update checker             |
| `REQUEST_INSTALL_PACKAGES`  | Installing a downloaded update APK                |

Sightings are stored under an anonymous Firebase account tied to this device
install. Anonymous auth doesn't survive an uninstall — see the code comment
on `AuthRepository` for the upgrade path to a persistent sign-in.

## Legacy web app (`android-app/`, `Car Log App.html`)

The original implementation was a self-contained HTML/JS app wrapped in a
Capacitor WebView shell, storing data in `localStorage` only. It's kept in
the repo for reference. To build it:

```
cd android-app
npm install
npx cap sync android
cd android
./gradlew assembleDebug
```
