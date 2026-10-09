# Stage 3 review

Scope: evidence-based English explanations and a dedicated result view over the existing local engine.

## Independent review and improvement

Initial critic assessment: **8.5/10 provisional**. Required correction: reopening a retained result from a composition effect could override Edit mode after activity recreation.

Improvement round 1: removed that effect. Analyse explicitly selects the result view; saved Edit/Back state is preserved. The real Activity/ViewModel instrumentation test now recreates both result and edit modes. Critic code reassessment: **9.0/10 provisional**, with no remaining Stage 3 code blocker. Final independent critic acceptance: **9.0/10**, after one improvement round and physical verification. No remaining Stage 3 must-fix. This score measures stage quality, not detection accuracy.

## Observed verification

- Implementation commit: `a74c3a2ca27bc1ba742145e9ca73083e96698e14`.
- [CI run 37969893156](https://github.com/ius-sharma/sentinel-android/actions/runs/37969893156): completed successfully. Engine tests, debug app build, instrumentation APK compilation and lint passed.
- Engine JUnit reports: **25 tests, 0 failures, 0 errors** (20 detection regressions and 5 explanation/rendering regressions).
- Lint report: **12 warnings**; not warning-free.
- CI artifact ZIP SHA-256 verified: `fa7b023434cacff329cbc00b3fcc61afdf83c568ac5d466e5f1684a0a34a7ce6`.
- Local APK: `artifacts/Sentinel-stage3.apk`; SHA-256 `fcffe78f6fd6b81681ee6e606412df73e1422068d3879139cb8087583066a25a`.

## Physical acceptance completed

- Physical Vivo V2117, Android 13/API 33: matching Stage 3 application and test APKs installed. Settings were backed up/restored after the CI debug signing-key change.
- `artifacts/stage3-instrumentation.txt`: **5 tests passed**, 0 failures. Foundation tests (2), result UI tests (2), real Activity/ViewModel scan test (1), including result/edit recreation, Android Back, result reopening, stale-verdict invalidation and clearing.
- Visually inspected `artifacts/stage3-partial.png`: high-risk request, partial coverage and 70 rule points explicitly distinguished from a probability.
- Inspected `artifacts/stage3-destinations.png`: full shortener host, submitted URL and unresolved-destination limitation readable; Clear scan accessible by scrolling.
- Inspected `artifacts/stage3-large.png` and `artifacts/stage3-large-scroll.png` with system font scale 1.3: summary and evidence wrap and scroll correctly. Original font scale 0.85 restored and verified.
- Manually pressed Android Back: original input and View last result retained.
- Critic independently inspected the instrumentation output and four screenshots; final rating **9.0/10**, accepted after one improvement round.

Stage 3 is complete within its defined scope. Verification covers one physical Android 13 device. TalkBack and API 26 runtime remain untested. Screenshot and test artifacts stay local/ignored; automatic notifications, QR/sharing and history remain later stages.