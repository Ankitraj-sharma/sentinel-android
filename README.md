# Sentinel

An Android app for on-device phishing and scam detection by Team PhishShield for Code Carnival 3.0.

## Planned scope

- Automatically analyse available content from supported notifications with user-granted access.
- Analyse messages, URLs, and decoded QR destinations on-device.
- Show English warnings with evidence and clear explanations.
- Provide manual scanning as a fallback and controls to pause monitoring and delete local history.

Notification access does not provide complete access to private chats or inboxes. Optional integrations will be documented separately.

## Status

Stage 1 Android foundation implemented: English onboarding, Protection / Scan / History / Settings navigation, dark and light themes, and locally persisted preferences.

**Detection is not implemented in this build.** There are no fabricated scan results or claims that protection is active. Notification selections and history preferences are saved for later stages; they do not enable monitoring.

## Planned stack

Kotlin, Jetpack Compose Material 3, Navigation Compose, and Preferences DataStore. The local threat-analysis engine follows in Stage 2.

## Development

### Requirements

- JDK 17 (set `JAVA_HOME` to its installation).
- Android SDK platform 35, Build Tools 35.0.0 and Platform Tools.
- Android 8.0 / API 26 or newer phone or emulator.

Open this folder in Android Studio and let Gradle sync. Android Studio creates the ignored `local.properties` file with your SDK location. For command-line builds, set `ANDROID_HOME` or add `sdk.dir=/path/to/sdk` to `local.properties`.

### Windows

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

### macOS / Linux

```sh
sh ./gradlew :app:assembleDebug :app:lintDebug
sh ./gradlew :app:installDebug
sh ./gradlew :app:connectedDebugAndroidTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Device commands require a connected, authorised device with USB debugging enabled, or a running emulator.

### Stage 1 acceptance checklist

- First launch explains this build's scope before entering the app.
- All four tabs open and Android Back works.
- Dark/light appearance updates immediately and persists after relaunch.
- Source selections and the history preference persist after relaunch.
- Protection remains explicitly inactive; Scan and History show honest empty states.
- Content scrolls on small screens and with enlarged text.
- No message, camera, notification or internet permissions are requested.

Instrumentation tests cover navigation truthfulness and persisted preferences. Visual and real-device verification are separate from compilation; see `docs/stage-1-review.md` for recorded evidence.

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

Only preferences are stored in app-private DataStore. History is off by default. Android backup is disabled. There is no internet permission, analytics SDK, message reader or notification listener in Stage 1. Future integrations must disclose actual access and any data sent externally.

## Attribution

AndroidX libraries are provided by the Android Open Source Project under their respective licenses. The Gradle wrapper is the standard Gradle tooling. Dependency coordinates and pinned versions are in `app/build.gradle.kts` and the root build file. The shield launcher asset is project-authored.

Do not commit credentials, API keys, signing keys, or real user messages. Credit external libraries and datasets when added.
