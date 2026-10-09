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

Build, engine tests, device results and final critic rating are pending. This file must be updated with observed results before Stage 2 is declared complete.
