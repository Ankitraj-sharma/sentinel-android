# Stage 6: private metadata history

History is off by default, with a new explicit consent dialog. An older Stage 1–5 placeholder save-history preference does not activate content retention. History records are typed: generated ID, timestamp, known source enum, risk enum, coverage enum, numeric rule score and known rule codes. No free-form message text, URL, domain, sender, OTP, image, matched excerpt or engine explanation can enter the persistence model. Saved metadata cannot reproduce full result evidence.

## Storage and consent

AES-256-GCM authenticated ciphertext is stored through Android AtomicFile in noBackupFilesDir. Keys are generated and used in Android Keystore; no embedded key, network call or added permission is involved. Hardware backing is device-dependent and is not claimed. The ciphertext envelope and parser are bounded at 128 KiB/100 typed records; malformed data or a missing key disables saving and exposes explicit reset, rather than silently recreating an enabled ledger.

The process-wide repository serializes consent and ledger changes with a mutex. Producers capture a consent-epoch ticket before analysis or notification preview queuing. Enabling later cannot save pre-consent work. Off, individual deletion and delete-all increment the epoch, so earlier queued work cannot resurrect metadata after deletion. A write already holding the mutex completes before deletion, which then removes it. Completed results can be seen even if saving fails; History reports storage errors separately.

- Turn off and delete removes the key and file; future saving stops.
- Delete entry removes one record. Delete all removes the current ledger entries but preserves enabled consent, so later analyses may create entries.
- Clear scan/warning removes current in-memory evidence. It does not delete separately saved metadata.
- Pause monitoring and clear session stops notification analysis and clears current scan/warning. It does not revoke Android access or delete saved metadata.

Retention is at most 100 entries, newest first. Expiry removes records older than seven days on initialization, foreground refresh, History open and recording. No scheduled background purge is implemented; the UI explicitly says expired metadata is removed on next app use/refresh. Clock changes can affect expiry and future-dated metadata is removed.

## Producer coverage

Manual/shared/QR analysis captures its source and consent before engine work. Editing imported content becomes Manual input. Notification previews capture consent on acceptance and save LIMITED metadata after supported analysis; hidden bodies and rejected input do not create entries. Existing package-selection and notification lifecycle guards remain in force. A result never implies its full content was saved.

## Evaluation

`sh ./gradlew :engine:evaluatePrototype` generates a complete diagnostic for 40 authored synthetic fixtures (16 SCAM, 16 BENIGN, 8 OUT_OF_SCOPE). HIGH/REVIEW count as warnings; UNKNOWN/NOT_ANALYSED are abstentions. Precision, conservative recall including scam abstentions, decision coverage and every case outcome are reported. Misses and false alarms are retained. Fixtures are visible during development and overlap rule design; this is not an independent representative accuracy benchmark. Stage 6 does not tune thresholds to improve the report.

Observed verification and independent review will be recorded in stage-6-review.md. References: [Android cryptography](https://developer.android.com/privacy-and-security/cryptography), [Keystore](https://developer.android.com/privacy-and-security/keystore), [AtomicFile](https://developer.android.com/reference/android/util/AtomicFile).
