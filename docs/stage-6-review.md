# Stage 6 review

Scope: explicit opt-in encrypted metadata history, deletion and session privacy controls, and transparent synthetic prototype evaluation.

Independent critic accepted **9/10** after **one implementation improvement round**, independently inspecting all five physical screenshots and the final phone transcript. Initial rating was 8.5/10. Improvements qualified expiry timing, corrected README persistence descriptions, and added actual producer tests. No remaining Stage 6 must-fix was identified.

## Verification

- [CI run 37990318096](https://github.com/ius-sharma/sentinel-android/actions/runs/37990318096), implementation/test commit `6d32bba95b405d5cfb1f75cc0598ff07476788b0`: build, lint, evaluation and APK generation succeeded. 35 engine tests passed with zero failures/errors. Lint reports 13 warnings. Builds ran in CI, not a local full SDK.
- Vivo V2117, Android 13/API 33: final transcript **OK (22 tests)** in 45.949 seconds. New tests verify actual Keystore encryption/recreation, cap/expiry, tamper recovery, consent/deletion races, UI consent/cancellation/deletion, all scan producers, and real OS synthetic notification metadata with history ON/OFF. Prior regressions also pass. The first full run had one assertion failure because a metadata rule label contains a bullet prefix; substring matching corrected it. That failed run is not counted as a pass.
- Physical UI verification at production-code commit `7f9ccc8` showed explicit consent before enabling, a shared-URL analysis saved as typed metadata, expanded score/rules without the destination, and pause-and-clear removing current input while preserving separately saved metadata. Turning history off returned to zero entries and removed the production encrypted file. The subsequent commit changes only the UI test assertion.

## Evaluation limits

The [complete generated diagnostic](stage-6-evaluation.md) reports 40 authored synthetic fixtures: 16 SCAM, 16 BENIGN and 8 OUT_OF_SCOPE. There were 12 true warnings, 3 false warnings, 4 misses and 13 true negatives. Warning precision was 80% and warning recall 75% on these authored labels; all eight out-of-scope cases abstained. These fixtures overlap rule development and are not an independent representative accuracy benchmark. No production accuracy or winning score is established, and thresholds were not tuned in Stage 6.

## Privacy and acceptance limits

History stores only typed time/source/risk/coverage/score/rule metadata, encrypted using Android Keystore AES-GCM in excluded-backup private storage. Raw messages, URLs, domains, OTPs, images and matched excerpts cannot enter the persistence model. Consent and deletion invalidate earlier queued saves; corrupted storage pauses saving until explicit reset. The cap is 100 entries, and expired entries are removed on next app use/refresh rather than by a background deadline.

Verification covers one Android 13 phone. Other Android versions, manufacturers and TalkBack have not been physically demonstrated for Stage 6. Hardware-backed key storage is not claimed. A missing warning or negative verdict never proves safety.

## Artifacts and restoration

Final CI ZIP SHA-256: `27ab5f4031cee45f15979d771b6a75cb9a5a875cd0dd7fdeb0948b2f579e94ea`.

Final installed APK SHA-256: `5e4b55c1146d1d6f3684a6edc4b976fbd3833825f41249f3d794fa8ba26f370f`.

Original preferences restored byte-for-byte: 133 bytes, SHA-256 `24c8fb98660982837a6121bed1092386f0c93d5c7b298b29263cf763c4d19a85`. Baseline and restored snapshots match: listener and alert permissions enabled, camera denied, font scale 0.85, rotation 0 and accelerometer rotation 0. Real sources were disabled during synthetic notification instrumentation. Synthetic history is OFF and its file absent; the fixture was force-stopped and temporary alert permission revoked, and the test APK/provider uninstalled.

Local ignored evidence: `stage6-final-instrumentation.txt`, `stage6-consent.png`, `stage6-history.png`, `stage6-metadata.png`, `stage6-pause-cleared.png`, `stage6-history-off.png`, and baseline/restored preference and state snapshots. Local APK: `artifacts/Sentinel-stage6.apk`. No real user messages were committed.
