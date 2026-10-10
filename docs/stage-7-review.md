# Stage 7 scoped hardening

Scope authorized: local detection improvements, independent synthetic evaluation, and
reliability/usability. The owner explicitly excluded real-phone tests and mentor feedback.
No device installation, phone test, mentor outreach or Play Protect appeal was performed.

## Engineering changes

- Extended credential requests to wallet seed/recovery phrases and private keys.
- Suppress indirect negation in combination rules (e.g. "never ask customers to buy").
- Detect work/task offers demanding payment to unlock earnings; ordinary payroll text is
  a separate benign development regression.
- Removed source notifications invalidate queued revisions and their dedup entry. A later
  repost can be assessed again. Undelivered pending warnings expire when their source is
  removed; posted Sentinel warnings survive the app's own source-notification cancellation.
- Visible ellipsis cut-offs add a fragment-only limitation instead of pretending full
  message coverage. Absence of an ellipsis never proves completeness.
- Protection title distinguishes alerts disabled and background protection stopped from
  full monitoring. Existing reconnect controls/generation handling remain; no new automatic
  reconnect behavior is claimed. Green/navy theme and owner logo were not changed.

## Evaluation boundaries

The independent author did not inspect engine rules/tests. Original 40-case balanced set
was frozen before engine changes, SHA256
`7a470e7f80b86550b6d6e552a698c0e4f174a9016e4bba52abf4eeda0528bec1`.
Baseline precision 71.4%, recall 25%, false-positive rate 10%. These small synthetic values
are not representative accuracy. Later reuse of the same set is diagnostic, not unseen
final evaluation; retain all misses and false alarms in generated reports.

Manual/shared full-text results, notification engine projections and decoded QR URL-only
results are reported separately. Notification projections do not test extraction, delivery,
permissions, dedup or source replacement; QR projections do not test camera/image decoding.
The evaluator uses the default rules engine without the bundled historical URL index.
Metrics must not be advertised as end-to-end phone performance or production accuracy.

Commands use `:engine:evaluateStage7` with `-PevaluationLabel=baseline|final|unseen|qr`
and optional `-PevaluationDataset=<resource filename>`. Reports include dataset hashes,
all observed outcomes, precision, recall, false-positive rate and abstentions.

## Critic review

Final offline verification: 48 engine tests pass; debug build and lint pass. No phone checks.

| Set | Cases | Precision | Recall | False-positive rate |
|---|---:|---:|---:|---:|
| Original baseline | 40 | 71.4% (5/7) | 25% (5/20) | 10% (2/20) |
| Original reused diagnostic | 40 | 83.3% (5/6) | 25% (5/20) | 5% (1/20) |
| Fresh unseen text | 24 | 100% (2/2) | 16.7% (2/12) | 0% (0/12) |
| Independent decoded QR URLs | 12 | 100% (3/3) | 50% (3/6) | 0% (0/6) |

These are distinct small datasets, not before/after comparisons across different rows.
Only the first two rows use the same cases. Low unseen text recall (10 of 12 scams missed)
is a material unresolved detection limitation; two true warnings do not establish reliable
precision. QR labels describe authored URL deception scenarios, not verified live malicious
sites. No sites were visited. There was no further rule tuning after these final results.
The fresh independent reports are [text](stage-7-unseen-evaluation.md) and
[decoded QR](stage-7-qr-evaluation.md); retain every miss and false alarm.

Round 1: 8.2/10. Round 2: 9.0/10 for scoped engineering and transparent evaluation after
fixing pending-warning expiry and adding further development regressions. The score is
not a detection accuracy rating. Low recall and lack of device verification remain material
limits. The reviewer accepted round 2 subject to final checks passing and honest disclosures.
