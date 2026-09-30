# London Underground Car Number Log

A pocket logbook for tube spotters. Type the number stencilled on the end of a
London Underground car and it records the sighting with a timestamp and your
GPS position, matches it against known rolling stock, and keeps a searchable
history — all offline, on your phone.

Built as a self-contained web app, and packaged as an Android APK so it can be
installed and used without a network connection.

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
- **CSV export** — download your full log as a CSV file.
- **Works offline** — data is stored locally on the device (`localStorage`);
  nothing is sent to a server.
- **Update check** — Settings shows the installed version and a "Check for
  updates" button that looks at this repo's latest GitHub Release; if it's
  newer, it offers the APK to download. See [Releasing an update](#releasing-an-update).

## Repository layout

```
Car Log App.html     Self-contained web app (HTML/CSS/JS, no build step)
android-app/          Capacitor project that wraps the web app for Android
  www/index.html       The web app as packaged into the native app
  android/             Native Android (Gradle) project
CarLog.apk            Prebuilt debug APK, ready to install
```

## Installing on Android

Grab [`CarLog.apk`](CarLog.apk) and either:

- Copy it to your phone (USB, email, cloud drive, etc.) and open it — you'll
  be prompted to allow installing from that source, or
- With the phone connected over USB with debugging enabled:
  ```
  adb install CarLog.apk
  ```

This is a debug build signed with a debug key, which is fine for installing
on your own device but not for distribution through the Play Store.

On first launch, allow the location permission prompt so sightings get GPS
coordinates — the app works without it, sightings will just be recorded
without a location.

## Building from source

Requirements:

- Node.js
- A JDK compatible with the Android Gradle Plugin in use (JDK 21 is known to
  work; very new JDKs may fail with `Unsupported class file major version`)
- Android SDK (platform 34+, build-tools, platform-tools), with
  `ANDROID_HOME`/`ANDROID_SDK_ROOT` set and licenses accepted

```
cd android-app
npm install
npx cap sync android
cd android
./gradlew assembleDebug
```

The APK is written to `android-app/android/app/build/outputs/apk/debug/app-debug.apk`.

To change the web app itself, edit `Car Log App.html` and copy it over
`android-app/www/index.html`, then re-run `npx cap sync android` and rebuild.

## Releasing an update

The in-app update check compares its own `APP_VERSION` constant (in the
`Component` script inside `Car Log App.html`) against the `tag_name` of this
repo's [latest GitHub Release](../../releases/latest), and offers whichever
asset in that release matches `*.apk`. To ship an update:

1. Bump `APP_VERSION` in `Car Log App.html` (and copy it to
   `android-app/www/index.html`).
2. Bump `versionCode`/`versionName` in
   `android-app/android/app/build.gradle` to match.
3. Rebuild (`npx cap sync android && ./gradlew assembleDebug`) and copy the
   resulting APK to `CarLog.apk`.
4. Commit, push, then tag a new GitHub Release named `vX.Y` (matching
   `APP_VERSION`) with `CarLog.apk` attached — e.g.
   `gh release create vX.Y CarLog.apk`.

Version comparison is numeric per dot-separated segment (`1.10` > `1.9`), and
the `v` prefix on the tag is ignored.

## Permissions

| Permission             | Why                                              |
|-------------------------|---------------------------------------------------|
| `ACCESS_FINE_LOCATION`  | Tag sightings with GPS coordinates                |
| `ACCESS_COARSE_LOCATION`| Fallback location accuracy                        |
| `VIBRATE`               | Haptic feedback on keypad taps                    |
| `INTERNET`              | Required by the Capacitor/WebView runtime         |

No data leaves the device — these permissions are used entirely locally.
