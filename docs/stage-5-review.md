# Stage 5 review

Scope: local QR camera capture, bounded selected-image QR decoding, plain-text Android share reception and explicit review before analysis.

Independent critic accepted **9.0/10**, after **two review/improvement rounds**, at implementation commit `635adef61bcb7f6c0fea0ac389f7628be81ab514`. Initial rating was **8.5/10 provisional**. Required fixes were empty/error/busy import recovery through Clear and meaningful tests of actual bounded content-URI reading. Both are closed; no remaining Stage 5 must-fix was identified.

## Observed verification

- [CI run 37986310219](https://github.com/ius-sharma/sentinel-android/actions/runs/37986310219): engine tests, app/test/fixture builds and lint succeeded. **32 engine tests, zero failures/errors**. Lint has **13 warnings**, not a warning-free result. Builds ran in CI; no local full-SDK build is claimed.
- Vivo V2117 Android 13/API 33: final transcript **OK (16 tests)** in 32.874 seconds. Five image/share ingestion tests exercise actual QR pixels, blank/ambiguous QR rejection, a real content URI, encoded-byte/dimension/format guards, missing/disallowed URI handling and merged manifest permissions. Two share UI tests verify cold/warm Android activity dispatch, explicit review, stale verdict replacement, edit retention across recreation, oversized-content rejection and Clear recovery. The prior nine foundation/scan/result/notification tests also pass, including synthetic real-OS notification delivery.
- Manual physical camera capture of a generated synthetic laptop QR produced **QR CAMERA · High risk**, rule score 85 explicitly labelled not a probability. The destination was `https://paypal.com@evil.test/login`; the result explains the obscured destination. Camera permission denial was separately forced through revoked/user-fixed permission state, and fallback controls plus Manage camera permission remained visible.
- Android OpenDocument selected the synthetic PNG in Downloads. Import displayed the complete URL with **QR image · Review before analysis**, without a verdict. Only pressing Analyse locally produced **QR IMAGE · High risk**. Picker Back retained prior input. A synthetic UPI QR replaced previous input/verdict with a clear unsupported error, disabled Analyse and enabled Clear.
- External ADB cold ACTION_SEND delivered fixed synthetic text to the review screen without automatic analysis. Android query-activities confirms Sentinel is registered as a text/plain share target. This does not establish the behavior of every third-party app's share payload.
- Critic independently inspected camera/result, denial, image review/result, unsupported QR and external-share screenshots, final transcript and restoration evidence.

Earlier verification failures are recorded rather than counted as passes: CI caught a protected lifecycle method call in a test; physical tests caught a standalone Kotlin test provider lacking target-runtime dependencies and ActivityScenario tracking a MAIN launch after a SEND replacement. Tests now dispatch actual activity intents from a cold share launch; the standalone provider uses only Android/Java and a fixed generated PNG asset. The final full run passed after these corrections.

## Artifacts and restoration

CI ZIP SHA-256: `dd0ecc484ab5cf7a200931c8578547bdf293d1dfee86bdc0b9348aeed0472d14`.

Installed final app APK SHA-256: `17ca43cf9c7ab016958eb1e5ddcd6d36fc3d1478901ba25019f41b308d2a4a80`.

Original preferences restored byte-for-byte: 133 bytes, SHA-256 `24c8fb98660982837a6121bed1092386f0c93d5c7b298b29263cf763c4d19a85`. Baseline and final grant/display snapshots match: original listener access and alert permission enabled, camera denied, font scale 0.85, rotation 0 and accelerometer rotation 0. The user had enabled notification access during their Stage 4 manual test, so their actual prior setup was restored. Real sources stayed disabled during synthetic notification instrumentation. The fixture was force-stopped and its temporary alert grant revoked; the test-only APK/provider was uninstalled after verification.

Ignored local evidence includes `stage5-final-instrumentation.txt`, `stage5-camera-result.png`, `stage5-camera-denied.png`, `stage5-image-review.png`, `stage5-image-result.png`, `stage5-unsupported.png`, `stage5-share-review.png`, and baseline/restored state snapshots. Final local app: `artifacts/Sentinel-stage5.apk`. The two generated QR PNGs in phone Downloads are available for manual testing; no real image or message was committed. Picker inventories were removed after locating the synthetic files.

## Acceptance limits

The score covers implementation quality and demonstrated synthetic workflows, not real-world scam accuracy. Camera/picker behavior is verified on one Android 13 phone; API 26, other manufacturers, TalkBack and Stage 5 enlarged-font interactions are not physically demonstrated. QR scanning assesses HTTP(S) payloads only, not payment/Wi-Fi/contact/app commands. Offline rules do not resolve redirects or fetch website reputation. Cancellation prevents stale result publication; a blocking provider read can continue briefly. Scan content/results remain in memory; persistent history and initial measured evaluation belong to Stage 6.
