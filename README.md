# Stool Sentry

![Stool Sentry icon](docs/icon.png)

Offline bowel movement tracker for Android. Stool Sentry keeps a private calendar of logs, records each one on the Bristol scale, and shows trends over time. It is free, with no ads, no account, and no network access.

Copyright (C) 2026 squishey123. Licensed under [GPL-3.0](LICENSE).

## What it does

- A month calendar marks days that have a log, with a count when a day has more than one. A day with no log means nothing was recorded.
- Logging takes four steps: Bristol type, size, color, then date, time, symptoms, and an optional note. The date and time start at now, or at the day you selected.
- Tap a day to see that day's logs, then view, edit, or delete them.
- The summary covers 7 days, 30 days, 90 days, and all time: counts, averages, gaps, and the mix of type, size, color, time of day, and symptoms.
- Export and import a CSV file from Settings. That file is the backup.
- An optional app lock asks for the device screen lock or biometrics when the app opens.

## Install

Download `stool-sentry-1.0.0.apk` from the [GitHub Release](https://github.com/squishey123/Stool-Sentry/releases) and install it. Android will ask you to allow installs from your browser or file manager. The release APK is signed. A debug build uses a different key, so it will not update an installed release.

## Build

You need JDK 21 and the Android SDK. From this folder:

```bash
./gradlew test assembleDebug
```

On Windows, use `gradlew.bat`. The folder path may contain spaces.

`gradle.properties` sets `org.gradle.java.home` for this development machine. GitHub Actions removes that line and uses the runner's JDK 21. Delete the line locally if it points at a JDK you do not have.

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Releases

Pushes to `main` and pull requests run unit tests and `assembleDebug`. A tag named `v*` runs the tests, builds a signed release APK, and attaches `stool-sentry-{version}.apk` to a GitHub Release. The job fails if the tag does not match `VERSION_NAME` in `version.properties`.

The release workflow does not write release notes. Those are added after the build succeeds.

The signing key is not in git. GitHub Actions reads it from repository secrets, and a local copy of `release.keystore` and `keystore.properties` is the backup. Both files are gitignored. If the secrets and the local copy are both lost, a later APK cannot update an already installed release.

## Privacy

Stool Sentry does not request internet access. It has no account, no ads, and no analytics. Logs live in a database on the phone.

Android backup and device transfer skip that database. Uninstalling the app deletes every log. Export a CSV if you want a copy.

The database file is not encrypted. App lock only hides the screens behind the device lock. The exported CSV is plain text, so keep that file somewhere private.

## Medical disclaimer

Stool Sentry is a personal log. It is not a medical device and does not diagnose, treat, or prevent any condition. The summary note about red, maroon, or black stool, or blood, is general information only. Talk to a qualified clinician about symptoms that worry you.

## Fonts

Playfair Display and Manrope are bundled under the SIL Open Font License. See `third_party/fonts/`.
