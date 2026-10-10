# Telegram notification support (0.7.4)

Telegram is a separate opt-in source, initially OFF for new and existing users. Enable it
in Protection or Settings after notification access and monitoring setup. Only newly
delivered readable previews are analysed: full messages/chat history, muted notifications,
hidden previews and an open chat with no notification are not covered.

Supported exact packages: `org.telegram.messenger` (stable Play Store client) and
`org.telegram.messenger.web` (telegram.org client). Telegram X, beta and third-party clients
are not included. Package selection is routing, not cryptographic publisher verification.
Official package reference: https://core.telegram.org/reproducible-builds

The existing listener pipeline applies the same local engine, consent gates, duplicate and
revision handling, professional warning, source-notification replacement after successful
alert delivery and encrypted opt-in metadata history. Source labels identify Telegram.
No Telegram login, bot, API integration or new Android permission is added.

Regression tests cover exact source filters and isolated DataStore consent persistence,
including independence of existing Gmail/WhatsApp/SMS/monitoring choices. Synthetic pipeline
tests disable Telegram during their fixture-only run and restore its prior setting.

Verification: engine suite 43 tests passed; debug app/test APK build and lint succeeded.
On Vivo Android 13, isolated `.telegramqa` TelegramSettingsTest passed (1 test, 0.323s).
Run this test only with `-PisolatedValidation=true -PvalidationSuffix=telegramqa` and
instrumentation class filter `com.phishshield.sentinel.TelegramSettingsTest`; its guard
intentionally prevents it from changing regular user settings. Temporary QA packages
were removed. User's `.validation` app updated in place to 0.7.4 and launched successfully.
Independent critic: round 1 provisional 8.5/10; round 2 final 9.1/10 after validation.

Actual Telegram incoming-message detection remains a separate manual check: enable Telegram,
go Home with the chat closed, send a controlled suspicious message from a second account,
check the warning and Telegram attribution, then try a benign message and source-OFF case.
Do not describe policy/settings tests as a real Telegram end-to-end test.
