# Stage 1 review

## Scope

Android foundation, English onboarding, navigation, persistent preferences, and honest inactive feature states. Detection is outside this stage.

## Critic review 1

Independent critic: provisional source rating **8.0/10**. Build and runtime evidence were pending. This is not a verified final rating.

Changes made in improvement round 1:

- Preference rows are full-width accessible switches with merged labels and summaries.
- Settings writes run in a ViewModel scope to survive Activity recreation.
- Successful saves clear previous save errors.
- Onboarding shows save errors and lets the user retry.
- Scan and History describe feature availability without developer stage numbers.

## Critic review 2

Independent critic reassessment: **9.0/10 after one improvement round**. The critic inspected revised sources, five physical-device screenshots and instrumentation output. No further implementation iteration was required for Stage 1. This rating applies to the Android foundation, not to detection accuracy or the complete product.

## Verified results — 9 October 2026

- Source build: `9232dd50fc7a8e535b10b083d0d0c361cec65ad4`.
- [GitHub Actions run](https://github.com/ius-sharma/sentinel-android/actions/runs/37963758068): `assembleDebug`, `lintDebug` and `assembleDebugAndroidTest` passed.
- Lint: zero errors, 12 warnings (including target/dependency freshness and future data-extraction configuration). No lint baseline or error suppression added.
- CI artifact SHA-256 checked before extracting: `9a42622585c3b560e8c8e2f6bcb5fb378e9067a00da2eb122f15977a0c3b0895`.
- Physical device: Vivo V2117, Android 13 / API 33, 1080 × 2400.
- App and test APKs installed successfully; MainActivity cold launch succeeded.
- Instrumentation: **OK (2 tests)**, covering four-tab navigation with truthful inactive states and preference persistence across repository recreation.
- Welcome, Protection, Settings dark/light and enlarged-text screens visually inspected on the physical device.
- Full-row dark-theme and WhatsApp toggles changed their checked states. Both persisted after force-stop/relaunch. Onboarding stayed completed.
- History preference was enabled and its checked state persisted after force-stop/relaunch. It was restored to disabled; the History screen showed the corresponding disabled preference and honest empty state.
- Android Back returned from Settings to Protection.
- Font scale temporarily changed to 1.3 for layout verification and restored to the device's original 0.85. Content remained readable and scrollable; navigation remained visible.
- Installed requested permissions contained only the AndroidX app-private dynamic-receiver signature permission. No internet, SMS, camera or notification permissions were present.

Local validation outputs are ignored under `artifacts/`: instrumentation log, UI hierarchy XML, lint report, APKs and screenshots. They are not committed as user data.

## Coverage limits

One physical device was verified. Screen-reader interaction, rotation-specific interaction and API 26 runtime have not been demonstrated. Detection, notification monitoring, QR decoding and actual history storage remain future stages. Local compilation initially hit a Gradle cache rename failure under OneDrive; successful compilation is established by CI, not by that failed local attempt. Local tooling downloads also encountered timeouts; Android SDK setup on this host is not a verified completion claim.

## Device acceptance procedure

1. Install the debug APK on a clean device/app data state.
2. Confirm welcome copy, continue, and inspect inactive protection.
3. Navigate every tab and use Android Back.
4. Toggle appearance, source selections and history preference.
5. Force-stop and relaunch; verify values and onboarding persistence.
6. Recreate the Activity; verify navigation and saved settings.
7. Inspect light/dark modes and enlarged-text layout.
8. Confirm no message, internet, notification or camera permissions in the merged manifest.
9. Run `connectedDebugAndroidTest`; inspect output rather than assuming a pass.

Do not record synthetic results as live runtime evidence.
