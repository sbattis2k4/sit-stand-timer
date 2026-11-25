# Sit/Stand Timer

A simple, lightweight Android timer app that reminds you when to sit and when to stand while you work.

## Features

- **Configurable Sit & Stand Durations**: Adjust each interval from 1 to 240 minutes (`-5`, `-1`, `+1`, `+5`) or use quick presets (`30m / 45m / 60m` for Sit, `15m / 30m / 45m` for Stand).
- **Auto-Advance**: Optionally start the next interval automatically, or pause between transitions.
- **Lock Screen Live Countdown**: Runs as a foreground service with a persistent, silent lock-screen notification showing your current mode (`SIT` / `STAND`), a live ticking countdown, and **Switch Now** / **Stop** actions.
- **Switch Reminder Notifications**: High-priority alert when it's time to sit or stand, backed by `AlarmManager` for reliable timing during Doze.
- **Two Home Screen Widgets**:
  - **Sit/Stand Timer (Compact)**: Live countdown, tappable `SIT ⇄` / `STAND ⇄` mode pill, and Start/Stop button.
  - **Sit/Stand Timer (Wide)**: Dedicated **Sit** and **Stand** mode buttons, live countdown, and Start/Stop button.

## Install

Download `sit-stand-timer.apk` from the [Releases](https://github.com/sbattis2k4/sit-stand-timer/releases) page on your Android phone (Android 8.0 / API 26+) and open it to install.

## Building from Source

1. Set up `local.properties` with your Android SDK path (`sdk.dir=...`).
2. Build a debug APK and run unit tests:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
3. To build a signed release APK (`app/build/outputs/apk/release/sit-stand-timer.apk`), add a `keystore.properties` file at the project root:
   ```properties
   storeFile=../keystore/release.jks
   storePassword=your-store-password
   keyAlias=sitstand
   keyPassword=your-key-password
   ```
   Then run:
   ```bash
   ./gradlew assembleRelease
   ```
