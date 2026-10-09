# Sentinel

An Android app for on-device phishing and scam detection by Team PhishShield for Code Carnival 3.0.

## Planned scope

- Automatically analyse available content from supported notifications with user-granted access.
- Analyse messages, URLs, and decoded QR destinations on-device.
- Show English warnings with evidence and clear explanations.
- Provide manual scanning as a fallback and controls to pause monitoring and delete local history.

Notification access does not provide complete access to private chats or inboxes. Optional integrations will be documented separately.

## Status

Stage 2 local analysis implemented on the Android foundation: English onboarding, four-tab navigation, dark/light themes, locally persisted preferences, and manual message/URL analysis with a pure Kotlin rules engine.

**Automatic protection is not active.** Manual Scan produces actual rule-based results and explanations. It does not prove safety or use a trained ML model. Notification selections and history preferences remain future settings; they do not enable monitoring or save scans. QR and Android sharing follow later.

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

### Stage 1 acceptance checklist

- First launch explains this build's scope before entering the app.
- All four tabs open and Android Back works.
- Dark/light appearance updates immediately and persists after relaunch.
- Source selections and the history preference persist after relaunch.
- Automatic protection remains explicitly inactive; History has an honest empty state.
- Manual Scan handles suspicious and legitimate messages, explains evidence, and treats unresolved destinations as inconclusive.
- Editing or clearing a message removes its previous verdict; messages/results are not written to history.
- Content scrolls on small screens and with enlarged text.
- No message, camera, notification or internet permissions are requested.

Engine tests cover benign and adversarial inputs. Instrumentation covers navigation, preference persistence and the real ViewModel-backed scan/edit/clear flow. Visual and real-device verification are separate from compilation; see [Stage 1 review](docs/stage-1-review.md) and [Stage 2 review](docs/stage-2-review.md).

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
