# Stage 4 review

Scope: opt-in supported-notification preview analysis, permission/status setup, generic local warning alerts and transient evidence navigation.

Independent critic accepted **9.0/10**, after three improvement rounds. Initial rating was **8.5/10 provisional**. Fixes cover dedup/queue reset on disconnect/reconnect, permission/current-token checks before posting, dynamic default SMS ownership, renewed alerts for distinct warnings and narrow SMS handler visibility. No remaining Stage 4 must-fix was identified.

## Observed verification

- Verified implementation: `3cc3dba91f7c1af3f6bc746b7fb7fee65cd4e4c4`.
- [CI run 37977547120](https://github.com/ius-sharma/sentinel-android/actions/runs/37977547120): build, engine regressions, lint, app/test APK and separate fixture APK succeeded. **29 engine tests, zero failures/errors**. Lint has **12 warnings**, not a warning-free result. Builds ran in CI; no local full Android SDK build is claimed.
- Vivo V2117, Android 13/API 33: final instrumentation transcript reports **OK (9 tests)** in 24.87 seconds. Actual Android listener delivery verified high-risk warning/alert, identical-update suppression, changed-update reanalysis, benign/inconclusive/hidden previews, pause, source selection, summary/ongoing filtering, access revocation and same-content reconnect. SMS metadata test resolves the same owner as the system SMS role without reading messages. Existing foundation, scan and evidence UI tests also passed.
- Manual physical checks: actual system-tray alert tap opened the synthetic notification evidence view; revoked alert permission preserved an in-app warning with truthful disabled-delivery status; font scale 1.3 kept protection controls readable. Original font scale 0.85 restored.
- Real WhatsApp, Gmail and SMS analysis stayed OFF throughout authorized synthetic monitoring. Original settings restored byte-for-byte (SHA-256 `1d4c00b5dbe30dd889855ae4a61b8689883a1a7db0f9dbb0c5f931c5ca001067`). Temporary listener and alert grants revoked; monitoring remains off and fixture force-stopped.

The first runtime attempts exposed publisher warm-launch handling and notification pacing issues in the test fixture. These were corrected with singleTop/onNewIntent and paced updates. A later compatibility regression found a null SMS-owner API result with SMS_DELIVER visibility alone; the narrow SENDTO/smsto query corrected this, and the unchanged strict role comparison then passed. Earlier failures are not counted as successful tests.

## Reproducible artifacts

CI artifact ZIP SHA-256: `b1434fff35cbc19547a6ea95c1a3e407cf6a95cf66901a97836150eff0ae14ca`.

Installed final app APK SHA-256: `2ae8e0a853c79d2ddff67f3b9b46a33d28fbedd97a9d7ed290ee183f9def7bb3`.

Ignored local evidence: `artifacts/stage4-final-compat-instrumentation.txt`, `stage4-warning.png`, `stage4-denied-result.png`, `stage4-large.png`, final app `Sentinel-stage4.apk`, and separate publisher `Sentinel-synthetic-demo.apk`. No real notification bodies were committed.

## Acceptance limits

Synthetic events delivered through the real Android OS verify pipeline/lifecycle behavior; they do not establish live WhatsApp/Gmail preview compatibility or production detection accuracy. Every analysed notification is labelled partial coverage. API 26, Android 15 redaction, other manufacturers and TalkBack were not physically verified. Persistent history, QR/share input, measured evaluations and broader hardening remain later stages. A rule score is not a calibrated probability of fraud.
