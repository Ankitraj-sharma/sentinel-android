# Sentinel

An Android app for on-device phishing and scam detection by Team PhishShield for Code Carnival 3.0.

## Planned scope

- Automatically analyse available content from supported notifications with user-granted access.
- Analyse messages, URLs, and decoded QR destinations on-device.
- Show English warnings with evidence and clear explanations.
- Provide manual scanning as a fallback and controls to pause monitoring and delete local history.

Notification access does not provide complete access to private chats or inboxes. Optional integrations will be documented separately.

## Status

Stage 5 is verified: camera QR scanning, bounded picker-selected QR images and plain-text Android sharing feed an explicit review screen before local analysis. Independent critic accepted 9/10 after two review/improvement rounds; 32 engine tests and 16 physical-phone tests passed. See [Stage 5 review](docs/stage-5-review.md) and [input paths](docs/stage-5-input.md). Stage 4 notification protection remains available and its regressions also passed.

Automatic analysis requires explicit Enable local monitoring, Android notification access and a selected source. WhatsApp/Business, Gmail and the current default SMS app are supported by exact package selection. Only newly delivered readable previews are analysed; full chats/inboxes, hidden content, live website reputation and guaranteed alert delivery are unavailable. Android 15 may redact OTP notifications. Manual Scan, QR camera/image and shared-text input are available. QR supports HTTP(S) destinations only; links are never opened automatically. Rules do not prove safety or use a trained ML model. Persistent history follows later.

## Planned stack

Kotlin, Jetpack Compose Material 3, Navigation Compose, Preferences DataStore, and the platform-independent `engine` module. See [engine design and limitations](docs/stage-2-engine.md).

## Development

### Requirements

- JDK 17 (set `JAVA_HOME` to its installation).
- Android SDK platform 35, Build Tools 35.0.0 and Platform Tools.
- Android 8.0 / API 26 or newer phone or emulator.

Open this folder in Android Studio and let Gradle sync. Android Studio creates the ignored `local.properties` file with your SDK location. For command-line builds, set `ANDROID_HOME` or add `sdk.dir=/path/to/sdk` to `local.properties`.

### Windows

```powershell
.\gradlew.bat :engine:test :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

### macOS / Linux

```sh
sh ./gradlew :engine:test :app:assembleDebug :app:lintDebug
sh ./gradlew :app:installDebug
sh ./gradlew :app:connectedDebugAndroidTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Device commands require a connected, authorised device with USB debugging enabled, or a running emulator.

### Current acceptance checklist

- First launch explains this build's scope before entering the app.
- All four tabs open and Android Back works.
- Dark/light appearance updates immediately and persists after relaunch.
- Source selections and the history preference persist after relaunch.
- Automatic monitoring remains paused until opt-in; listener access/connection and system alert delivery are separate visible states.
- Manual Scan handles suspicious and legitimate messages, explains evidence, and treats unresolved destinations as inconclusive.
- Editing or clearing a message removes its previous verdict; messages/results are not written to history.
- Content scrolls on small screens and with enlarged text.
- Optional notification-listener access and alert permission are explained and requested separately. Camera permission is requested only for QR camera scanning. No SMS inbox, broad image/storage, accessibility-service or internet permission is requested.

Engine tests cover benign and adversarial inputs, explanation uncertainty and safe rendering of control characters. Instrumentation covers navigation, preference persistence, the real ViewModel-backed scan/edit/Back/clear flow, partial-coverage evidence and rejected input. See [Stage 1 review](docs/stage-1-review.md), [Stage 2 review](docs/stage-2-review.md) and [Stage 3 review](docs/stage-3-review.md) for observed results; test coverage is not measured detection accuracy.

## Delivery stages

1. Android foundation, navigation and settings.
2. Local message and URL detection engine.
3. Evidence-based English explanations and result screen.
4. Automatic supported-notification analysis and alerts.
5. QR and manual/shared input paths.
6. Privacy controls, local history and initial prototype evaluation.
7. Mentor-driven hardening and final evaluation.
8. Pitch, demonstration and submission.

Stages 1–6 form the team's pre-event readiness milestone, estimated at 75%; this is not an official scoring formula.

## Privacy in this build

Only preferences are stored in app-private DataStore. Current manual/shared/QR scans and the latest notification warning stay in memory. Process death loses results, while configuration changes retain them. QR camera frames and chosen images are not saved; no persistent picker URI grant is retained. Pause/source policy changes or listener disconnection clear the latest notification warning and its alert. Android delivers notification events broadly; unselected package previews are discarded before text extraction. Pause does not revoke Android access; use Manage notification access to revoke it. No network operation, analytics or persistent message history is used. Android backup is disabled.

## Attribution

AndroidX libraries are provided by the Android Open Source Project under their respective licenses. The Gradle wrapper is the standard Gradle tooling. Dependency coordinates and pinned versions are in `app/build.gradle.kts` and the root build file. The shield launcher asset is project-authored.

QR decoding uses [ZXing Core](https://github.com/zxing/zxing) and [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded), licensed under Apache 2.0. The test QR image was generated from fixed synthetic text with ZXing and contains no real user data.

Do not commit credentials, API keys, signing keys, or real user messages. Credit external libraries and datasets when added.
