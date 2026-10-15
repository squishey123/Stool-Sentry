# Stool Sentry

![Stool Sentry icon](docs/icon.png)

Stool Sentry is a private bowel movement tracker for Android. It keeps a calendar of your logs, records each one on the Bristol scale, and shows trends over time. It is free, with no ads, no account, and no internet access.

Copyright (C) 2026 squishey123. Licensed under [GPL-3.0](LICENSE).

## What it does

- A month calendar marks days that have a log, with a count when a day has more than one. A day with no log means nothing was recorded.
- Logging takes four steps: Bristol type, size, color, then date, time, symptoms, and an optional note. The date and time start at now, or at the day you selected.
- Tap a day to see that day's logs, then view, edit, or delete them.
- The summary covers 7 days, 30 days, 90 days, and all time: counts, averages, gaps, and the mix of type, size, color, time of day, and symptoms.
- Export and import a backup from Settings. With encryption on, that file needs your passphrase. A readable CSV is still available as a separate export.
- An optional app lock asks for your device screen lock or biometrics when the app opens.

There is no "no bowel movement" choice. Leaving a day empty already means nothing happened.

## Install and updates

Download `stool-sentry-1.0.0.apk` from the [GitHub Release](https://github.com/squishey123/Stool-Sentry/releases) and install it. Android will ask you to allow installs from your browser or file manager. The release APK is signed. A debug build uses a different key, so it will not update an installed release.

F-Droid is the intended place for later updates. The listing is published after the next release. Until then, install from the GitHub Release above. That later F-Droid build uses this same release key, so it can replace the GitHub install without an uninstall.

Your logs stay on the phone. Uninstalling the app deletes them. Export a backup from Settings if you want a copy before you switch devices or remove the app. With encryption on, that file opens with your passphrase.

## Build

You need JDK 21 and the Android SDK. From this folder:

```bash
./gradlew test assembleDebug
```

On Windows, use `gradlew.bat`. If `JAVA_HOME` is older than 21, set `org.gradle.java.home` in `%USERPROFILE%\.gradle\gradle.properties`. Leave that path out of the project `gradle.properties`, because the F-Droid buildserver checks out this file.

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Releases

A tag named `v*` must match `VERSION_NAME` in `version.properties`. The release workflow builds a signed APK and attaches `stool-sentry-{version}.apk` to the GitHub Release for that tag. F-Droid rebuilds the tag and publishes the GitHub APK only when the two builds match. `v1.0.0` stays a GitHub release. The first tag for F-Droid is the next one.

The signing key is not in git.

## Medical disclaimer

Stool Sentry is a personal log. It is not a medical device and does not diagnose, treat, or prevent any condition. The summary note about red, maroon, or black stool, or blood, is general information only. Talk to a qualified clinician about symptoms that worry you.

## Coming later

Photo attachments are planned and are not in this version. Also left for later: custom symptoms, a home-screen widget, optional sync you control, a clinic-ready PDF report, reminders, search, medication and food notes, a light theme, and more languages.

Urine tracking and any paid tier are not part of this app.

## Privacy

Stool Sentry does not request internet access. It has no account, no ads, and no analytics. Logs live in a database on the phone.

Android backup and device transfer skip that database. Uninstalling the app deletes every log. Export a backup, or turn on auto-export, if you want a copy.

Encryption is optional. When it is on, the database and the backup file are encrypted with a passphrase you choose. That passphrase opens a backup on a new phone. The phone's screen lock is separate, and changing it does not remove the logs. If you forget the passphrase, those encrypted copies cannot be opened. A separate export can still write a readable CSV, and that file is plain text.

When encryption is off, the database file is not encrypted and the exported CSV is plain text. App lock only hides the screens behind the device lock.

Database encryption uses SQLCipher. That library is free software under a BSD-style license; see `third_party/sqlcipher/`.

Stool Sentry is built so F-Droid can list it with no [Anti-Feature](https://f-droid.org/en/docs/Anti-Features/) flags. Those flags warn when an app behaves in ways that mainly serve the developer or a third party. Here is how this app stays clear of each one:

- **Ads** — The app contains no advertising, house ads, or ad SDKs.
- **Disabled Algorithm** — Releases are signed with modern APK signature schemes (v2 and v3) using SHA-256, not outdated or unsafe algorithms.
- **Known Vulnerability** — Dependencies stay on current, maintained releases so scanners do not flag known security issues in the built app.
- **Non-Free Addons** — The app does not promote proprietary apps, the Play Store, or non-free plugins.
- **Non-Free Assets** — Icons, artwork, and bundled fonts are free to use. The fonts (Playfair Display and Manrope) are under the SIL Open Font License; other media is original to this project or under a free license.
- **Non-Free Dependencies** — The app runs with free software only. It does not need Google Play services, Firebase, Maps, or other proprietary packages.
- **Non-Free Network Services** — There is no account, backend, or sync path that depends on a proprietary cloud service. Logs and backup files stay on your device.
- **No Source Since** — The full source for every release stays public under the GPL-3.0 license, so F-Droid (and anyone else) can keep building and updating it.
- **Tethered Network Services** — The app works fully offline with no server and no account. Nothing is locked to a single remote service.
- **Tracking** — There is no analytics, crash reporting, telemetry, or update check that phones home. The app does not request internet access at all.
