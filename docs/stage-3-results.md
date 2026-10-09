# Stage 3: English results and evidence

Scope: turn the Stage 2 engine's observed findings into an understandable, actionable Android result view. Detection weights and thresholds are unchanged. Notification monitoring, QR/sharing and persistent history remain later stages.

## Result flow

Analyse opens a dedicated result view inside Scan. Android Back and Edit message return to the unchanged input; View last result reopens its result. Editing invalidates the previous verdict. Clear scan removes the current input/result. Scan content remains in ViewModel memory, survives configuration changes, and is lost on process death; only small display controls use saved state.

The screen shows a labelled risk icon, English summary, coverage state, rule points, next action, optional verification checklist, individual evidence cards, full parsed hosts and submitted links, all limitations and an expandable message with exact-match highlights. The benign state avoids a verified-safe badge. Controls are full width and text can scroll/wrap with enlarged fonts. Headings and expandable-control states have accessibility semantics. No suspicious destination is opened or contacted.

## Fidelity and uncertainty

- A deterministic engine-module presenter maps risk and analysis status separately. High risk can still have partial coverage. An unanalysed input has no score.
- Scores retain the engine's strongest-contribution-per-rule semantics and 100-point cap. They are not probabilities, confidence estimates or accuracy measurements.
- Evidence and reasons come directly from engine signals. The UI does not invent sender identity, website reputation or a trusted-domain verdict.
- Every destination shows its full host, not a guessed organisation or a shortened registrable domain. Unsupported destinations remain unparsed; shortened links retain their explicit limitation.
- Invisible format and non-whitespace control characters become visible U+ labels in submitted text and evidence. Newlines and tabs remain. Unicode normalisation can change engine snippets; highlights mark only exact matches in the displayed submitted text, with this limitation disclosed.
- Verification guidance directs users to an official app or a previously known contact, never a link or phone number supplied by the suspicious message.

## Verification

New JVM regressions cover partial/high-risk coverage, benign/unknown language, rejected-input recovery, guidance and control-character rendering. Android instrumentation checks real scan/edit/Back/reopen/clear behavior plus partial-coverage evidence, expansion, destination and rejected/no-score states. Physical screenshots and enlarged-font inspection are tracked separately in the review record. TalkBack and API 26 runtime must not be claimed without actual testing.
