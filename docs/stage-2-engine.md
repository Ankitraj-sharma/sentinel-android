# Local analysis engine

## Scope and architecture

The `engine` module is pure Kotlin/JVM and does not depend on Android, perform DNS lookup, fetch URLs or call remote models. Android supplies text; the engine returns structured risk, per-rule evidence, parsed destinations, limitations and a suggested next step.

Manual Scan uses this engine through a ViewModel on `Dispatchers.Default`. Editing or clearing cancels the pending operation and invalidates the previous verdict. Inputs and results stay in memory, survive Activity recreation and are not persisted to history, backup, analytics or logs. Force-stop/process death clears them.

Automatic notification protection, QR decoding, Android Sharesheet input and persisted scan history remain future stages.

## Rules and provisional weights

| Signal | Points |
| --- | ---: |
| Request to disclose OTP/password/PIN/CVV or authentication code | 55 |
| User information before `@` obscures the actual URL destination | 50 |
| Prize claim combined with a payment request | 40 |
| Claimed/lookalike brand outside its configured example domains | 35 |
| Upfront processing/refund/release/activation fee | 25 |
| Numeric destination | 20 |
| Urgency | 15 |
| Account restriction threat | 15 |
| HTTP connection | 10 |

The strongest occurrence of each rule contributes once, regardless of repeated phrases or links. Totals cap at 100. Scores are **heuristic rule points, not fraud probabilities**. Thresholds are provisional: 65+ High risk, 25–64 Review recommended, below 25 No obvious threat detected unless parsing/language/destination coverage is incomplete. Incomplete low-scoring analysis is Unable to assess fully. Positive risk signals are retained even when another link cannot be assessed.

These values have not been calibrated against a representative real-world dataset. Regression test counts are not accuracy, precision or recall claims.

## URL handling

- HTTP(S) URLs, `www.` links, standalone domain inputs and inline bare links with the configured common suffixes are supported.
- The actual authority is parsed with `URI`; user information is separated from the host. Hostname case and IDN forms are normalised locally. Invalid ports, malformed authority and encoded/hidden authority characters are reported as unparsed.
- Domain matches use exact equality or a dot-delimited subdomain suffix, never an unbounded substring.
- A match to a configured brand domain does not override suspicious message content or verify the page.
- Shorteners are not expanded. Internationalised domains receive explicit limited-coverage treatment, not a claim that Unicode domains are malicious.
- Text matching uses NFKC and strips invisible formatting. Where this changes the text, the result discloses that snippets may be normalised.
- Email addresses are excluded from inline bare-domain extraction. Unrecognised inline suffixes require a full HTTP(S) URL.
- Punctuation trimming preserves balanced IPv6 brackets and valid path parentheses.

## Bounds and privacy

Blank input, input over 20,000 characters and more than 16 distinct extracted destinations are rejected without partial scoring. The UI refuses oversized replacements and visibly reports that the new input was not accepted; it never silently scans a truncated paste. Full message content is not logged or written to disk by the scan feature.

## Example brand directory

The injectable directory contains PayPal, SBI, Google and Microsoft examples. It is incomplete and is not a reputation service. Legitimate organisations use additional domains, and domains can change. Domain mismatch explanations disclose this limitation.

SBI's `sbi.bank.in` entry was checked against its [official migration notice](https://sbi.bank.in/redirect/) and [corporate portal](https://corp.sbi.bank.in/) on 9 October 2026. Legacy examples do not make any destination safe. Other initial brand roots are illustrative configuration; independently maintain the directory before production use.

## Known limitations

English patterns only; no semantic model, attachments, sender authentication, redirect resolution, live reputation, webpage inspection or exhaustive Unicode confusable detection. Context can produce false positives or misses, including genuine urgent account notices and non-standard scam wording. A clean result is never proof of safety. Numeric local-service URLs can receive a review recommendation when combined with HTTP.

## Verification

Run `:engine:test` for adversarial and benign regression tests. Run the Android build/lint and `connectedDebugAndroidTest` for app integration. Actual results and critic reassessment are recorded in `stage-2-review.md` once available.
