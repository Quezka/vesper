# Vesper

A bedside clock for Android, in the style of an iPhone's StandBy: when the phone starts charging it shows a big clock,
the date, the battery and the notifications that came in. Same look as Quire (indigo accent, flat cards).

- Kotlin + Jetpack Compose, `minSdk 26`.
- `domain/` is plain Kotlin (no Android imports; a test enforces it), `data/` holds settings, the release feed and the
  updater, `service/` the charger watcher and the notification reader, `ui/` the two screens.
- English, Russian and Italian; a test checks that every string exists in all three.

## Quire

With Quire (the planner, `github.com/Quezka/quire`, version 0.33 or newer) on the same phone, the clock also shows the
focus timer, what is on now or next, what starts tomorrow and the tasks due. Quire exposes it through a read-only content
provider behind a runtime permission (contract: Quire's `docs/bedside.md`, mirrored here in `domain/QuireFeed.kt` and
`data/QuireLink.kt`). Nothing goes through the cloud.

## Build

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Needs JDK 17+ and the Android SDK (`sdk.dir` in `local.properties`). The project path must be ASCII only.

## Permissions

Notification access (to show notifications), display over other apps or full-screen alerts (to open by itself),
notifications (the quiet "waiting for the charger" notice), and optionally unrestricted battery. Everything is
requested from the app's own screen, none of it at start-up.

## Releases and over-the-air updates

Pushing a tag `vX.Y.Z` (it must equal `versionName` in `app/build.gradle.kts`) builds and publishes
`Vesper-X.Y.Z.apk` on GitHub. The app's Updates card reads the latest release of `Quezka/vesper`, downloads that APK and
hands it to the system installer. The repository must be public for the app to read it without a token.

CI needs the secrets `VESPER_KEYSTORE_B64` (the keystore, base64), `VESPER_KEYSTORE_PASSWORD` and
`VESPER_KEY_PASSWORD`. Updates only install over the same signing key, so keep a backup of the keystore.
