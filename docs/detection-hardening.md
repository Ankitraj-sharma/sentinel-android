# Detection and monitoring hardening (0.7.0)

This update responds to manual false negatives, warnings on legitimate destinations, and missing automatic warnings. It remains an offline English rule engine with historical URL evidence, not a trained model or live reputation service.

## Detection changes

- Combination rules cover unrealistic investment returns, gift-card code requests, delivery problems with linked actions, tax refund claims with links, remote-access installation requests, and employment deposits. Routine mentions of these topics alone do not warn.
- HTTP and numeric-host traits remain visible evidence, but together contribute 20 points, below the 25-point warning threshold. This avoids labelling ordinary local HTTP/IP servers suspicious solely for those traits.
- Google service domains and PayPal's asset domain are recognised as configured brand infrastructure. Domain matching never suppresses suspicious message instructions or historical URL evidence. User-hosted pages on these domains can still be malicious.
- A standalone pasted URL keeps punctuation in its path/query; sentence extraction still trims prose punctuation.
- Inline bare hostnames are no longer restricted to a small suffix directory; a scheme-less `.br` destination in a notification is covered. Hostname-like prose/filenames can also be extracted; extraction alone does not cause a warning.
- Manual, shared, QR and automatic scans share the same lazily loaded offline index. Loading runs off the UI thread; a load failure produces a scan error rather than a clean verdict.

## Historical URL data

Source: [Malicious URLs dataset by Manu Siddhartha (sid321axn)](https://www.kaggle.com/datasets/sid321axn/malicious-urls-dataset), Kaggle version 1. Kaggle API metadata declares CC0: Public Domain. Raw source is kept in ignored local artifacts, never opened as websites.

The 651,191 rows contain 428,103 benign, 94,111 phishing, 32,520 malware and 96,457 defacement labels. The bundled sorted SHA-256 index contains 207,970 canonical malicious destinations. Canonical destinations with any benign label are omitted (4,044 conflicting keys), and 340 unsupported input rows are skipped. Multiple malicious labels use phishing, then malware, then defacement priority. Metadata includes source and output hashes in `app/src/main/assets/url-threats-v1.json`.

Canonicalisation ignores HTTP/HTTPS scheme and fragments, normalises hostname case and default ports, and preserves path/query case and encoding. A match applies to that canonical destination, never every page on its domain. It contributes 70 rule points with its historical label and a current-state caveat. A missing match does not establish safety. Some malformed dataset rows may remain unparseable by the scanner.

This is historical lookup evidence, not independent evaluation or generalisation to newly created URLs. Defacement is identified as defacement, not automatically described as phishing. The binary index is approximately 6.9 MB before APK compression and is shared in memory across scan producers.

Rebuild with `python tools/build_url_index.py artifacts/url-dataset/malicious_phish.csv`. No network is used by the script or the app.

## Automatic monitoring

- MessagingStyle previews are extracted on the entire supported API 26+ range, including when fallback text is missing.
- First delivered events can wait for initial settings loading instead of silently disappearing; the startup buffer is bounded and current source/consent policy still gates extraction.
- An already-authorised disconnected listener requests a throttled Android rebind when the app resumes, monitoring is enabled, or the service disconnects. Android controls whether it reconnects.
- Protection explains how to test a newly delivered background notification and distinguishes normal scans without alerts from absent/hidden previews. It exposes analysed/skipped counters and system alert state.
- Existing source filtering, opt-in, pause, revocation, generation invalidation, in-memory deduplication, and encrypted metadata-only history remain enforced.

No change grants notification access, reads a full chat/inbox, fetches websites, or bypasses hidden previews or device restrictions.

## Validation

Engine regressions exercise non-OTP scams, routine English messages, legitimate URLs, canonical path isolation, index corruption, punctuation and typed history metadata. Android regressions additionally exercise MessagingStyle-only content and real synthetic OS notifications for a gift-card scam, a dataset URL, and a legitimate local URL.

To protect an existing phone installation and its Keystore-backed history, build the app/tests/fixture with `-PisolatedValidation=true`. This gives each a separate package and QR-provider authority. Real notification sources remain deselected during the synthetic monitoring test. Synthetic matches and authored fixtures are development diagnostics, not production accuracy.

Stage 6 documents record historical results; the current generated fixture report is produced by `:engine:evaluatePrototype`.

Observed final verification on 10 October 2026:

- 42 engine tests passed, zero failures/errors.
- Final isolated debug APK, instrumentation APK and fixture build passed. Lint passed with 0 errors and the existing 13 warnings.
- Standard app/fixture debug builds also passed after isolated verification. Local artifacts are `artifacts/Sentinel-0.7.0.apk` and the separately installed `artifacts/Sentinel-0.7.0-preview.apk`; the main-package APK was not installed over the existing differently signed app.
- Vivo V2117, Android 13/API 33: final instrumentation transcript `OK (23 tests)` in 46.678 seconds. This includes actual OS synthetic notification delivery for a non-OTP gift-card scam, a scheme-less indexed URL, benign local URL handling, deduplication, pause, source deselection and revocation/reconnect.
- The first phone run passed, but lint subsequently caught an API 30-only platform helper in the attempted legacy extraction fix. That implementation was replaced with AndroidX compatibility extraction; the final build and full phone suite were rerun successfully. The initial run is not the final compatibility evidence.
- Original app settings and encrypted history were compared by byte count and SHA-256 before and after isolated testing, with matching hashes. No original app data or Keystore keys were reset. Temporary validation fixture/test packages were removed; Sentinel Preview remains installed for review.
- The existing installed main app uses a different signing certificate from the local build. Preview installs beside it; replacing the main installation without its original signing key would require uninstalling it and would endanger its saved history. That action was not performed.

Verification covers one phone and synthetic notifications. It does not establish successful delivery from a live WhatsApp/Gmail/SMS account under every device configuration, or real-world detection accuracy. The fixed authored fixture diagnostic reports 16 scam warnings and 2 benign false warnings; these known development fixtures overlap the new rules and are not a holdout evaluation. New/unlisted URLs, subtle contextual scams and platform-hidden previews remain limitations.

Domain-directory references: [Google's hostname allowlist](https://support.google.com/a/answer/6334001?hl=en-GB), [PayPal's own CSP troubleshooting guidance](https://slc-b-origin-pointofsale.paypal.com/us/cshelp/article/paypal-pay-links-and-buttons-troubleshooting-tips-help1137). Compatibility extraction uses [AndroidX MessagingStyle](https://developer.android.com/reference/androidx/core/app/NotificationCompat.MessagingStyle).
