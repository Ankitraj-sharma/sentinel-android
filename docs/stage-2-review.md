# Stage 2 review

Scope: local message/URL rules, bounded parsing, structured evidence and a manual Android scan path. Notification protection, QR, sharing, persistent history and ML are outside this stage.

## Independent critic review 1

Provisional rating: **8.2/10**, before runtime verification. Requested fixes: avoid matching disclosure requests across negative advice, detect inline bare domains and update the old Scan title assertion with a real ViewModel-backed integration test.

## Improvement round 1

- Credential rules constrain the verb-to-secret relationship and do not cross negative/clause connector words.
- Inline domain extraction excludes email addresses and is tested alongside scheme URLs and punctuation.
- Unicode-normalisation handling and its disclosed limitations were tightened.
- Existing navigation assertion updated; real-engine scan/edit/clear instrumentation coverage added.
- The actual `www.` host expectation was corrected rather than changing parser behaviour to strip it.
- Current SBI domain example added with a source, without treating domain matches as proof of safety.

## Verified acceptance

Final independent critic rating: **9.0/10**, accepted after one improvement round. No remaining blocker within Stage 2 scope. This rating measures stage engineering quality, not detection accuracy or contest readiness.

- Verified implementation: `53cababd177ec97abeeecc111156fb5d6bcbaf65`.
- [GitHub Actions run 37968140786](https://github.com/ius-sharma/sentinel-android/actions/runs/37968140786): successful engine tests, debug app build, instrumentation APK build and lint. Lint passed with 12 warnings.
- Engine JUnit report: **20 tests, 0 failures, 0 errors**. Includes benign advice, malicious contrast clauses, destination boundaries, malformed inputs and bounded analysis.
- Physical Vivo V2117, Android 13/API 33: **3 instrumentation tests passed** (two foundation tests and the actual scan/edit/clear integration test).
- Manual visual checks: high-risk secret request displays evidence; shortened `bit.ly` destination remains inconclusive with its limitation visible. Screens inspected at `artifacts/stage2-high.png` and `artifacts/stage2-shortlink.png`.
- Installed package requests no Internet permission. The scan feature has no network operation or persistent message history.
- CI artifact ZIP SHA-256 verified: `619fa6b0a83a53913a536296f5fef03cffe0ba9fbcbcad5d1efeafbd4e037452`.
- Verified APK: `artifacts/Sentinel-stage2.apk`, SHA-256 `283ca535a642f09e3b3330d40bd2254ee5e387397a0a080f53c122391256d685`. Installed on the phone; previous development settings backed up and restored after a debug signing-key change.

Artifacts containing device evidence remain local and ignored by Git. Automatic notification protection, QR/share ingestion and persistent history remain later-stage work. Rules and example brand coverage remain limited; heuristic scores are not calibrated probabilities.
