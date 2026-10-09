# Stage 4: local notification protection

## Consent and actual access

Monitoring defaults off. Setup separately explains local reading of selected previews, Android's broad notification-listener delivery, source selection, and alert permission. Pause stops analysis and clears the transient warning; it does not revoke the system grant. The app has no Internet, SMS inbox, contacts, camera or accessibility-service permission. Only settings are persisted. No active-notification backlog is replayed when access is granted or the service reconnects.

Exact sources: WhatsApp `com.whatsapp`, Business `com.whatsapp.w4b`, Gmail `com.google.android.gm`, and the current default SMS package resolved through Android. The default SMS package is rechecked before event acceptance and before committing an analysis. Group summaries, ongoing notifications and Sentinel's own alerts are ignored. Package selection does not authenticate the sender.

The manifest declares the narrow SMS_DELIVER package-visibility query required by [Android's default SMS package API](https://developer.android.com/reference/android/provider/Telephony.Sms#getDefaultSmsPackage(android.content.Context)) on Android 11+, plus SENDTO with the smsto scheme for SMS handler visibility. These expose package metadata, not permission to read the SMS inbox. Instrumentation compares the resolved owner with the system SMS role without reading messages.

## Pipeline and limits

System callbacks filter packages before extracting preview fields. Extraction combines bounded title/body/big-text/text-lines and MessagingStyle text (API 30+); older Android falls back to public text fields. Missing bodies and rejected extraction are counted as unassessed. At most 20,000 combined characters and 64 line/message entries are accepted; nothing is silently truncated by Sentinel. Android's own truncation/redaction cannot always be identified. Every analysed preview therefore has partial coverage.

A 16-item channel bounds pending work; full-queue events are skipped and counted. A sequential worker runs the existing engine on Dispatchers.Default. It checks current policy generation before and after analysis, so pause/source changes cannot commit previously queued results. The 128-key in-memory SHA-256 dedup cache suppresses identical updates for the same key while allowing changed content. Policy changes and disconnect/reconnect clear dedup and queued previews. This is bounded best-effort preview analysis, not a guarantee that every event is processed.

High/review outcomes create a single latest-warning memory record and a generic alert without message text. A new distinct warning replaces the previous one. Android notification/channel settings and Do Not Disturb may suppress delivery. After refreshing permissions, posting requires current access, connection, enabled policy and matching warning token. Alerts use immutable PendingIntents containing only a random memory token, never text or URLs. An expired/replaced token produces an unavailable notice rather than opening another warning. Links are never automatically opened.

## Separate synthetic fixture

`:fixture` is a debug-only separate Android publisher, with fixed scam, benign, changed, shortener, hidden, group and ongoing examples. Sentinel only selects its exact package in debug builds after Synthetic demo source is enabled. The fixture has no network permission and is not shipped as a production feature. It verifies actual Android callback/alert delivery; it does not simulate WhatsApp/Gmail internals or establish real-world detection accuracy.

Physical test prerequisites: install app/debug test APKs and fixture-debug APK; grant POST_NOTIFICATIONS to the app and fixture before running `MonitorPipelineTest` (API 33+). The user must authorize notification access testing. The test temporarily disables all real source analysis, permits only the fixture and restores original source/monitoring/access settings. Permission revocation can kill the app UID, so denied-alert testing runs separately from instrumentation. The fixture is force-stopped at test end.

The publisher handles warm launches through singleTop/onNewIntent; instrumentation paces updates below the system notification rate limit. A real system-tray tap is checked manually on the physical phone because OEM System UI node access is not portable. The automated test still asserts that the actual system alert has been posted with a content PendingIntent and no message body.

## Android evidence

Checked 9 October 2026: [NotificationListenerService lifecycle and registration](https://developer.android.com/reference/android/service/notification/NotificationListenerService), [Android alert permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission), and [Android 15 OTP redaction](https://developer.android.com/about/versions/15/behavior-changes-all#otp-redaction). Sensitive previews, work/private profiles, disabled notifications and manufacturer battery controls can limit coverage. Listener reconnection is offered when access remains granted but connection is absent.

Runtime evidence and independent critic acceptance are recorded separately in `stage-4-review.md`. Persistent history, QR/shared input, production evaluations and broader-device testing remain later work.
