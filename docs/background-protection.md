# Background protection and notification replacement (0.7.1)

Enabled monitoring now starts a user-visible foreground ProtectionService from the visible app or monitoring switch. A low-importance, ongoing notification states “Sentinel protection active” and includes Pause protection. The service continues when the activity task is removed, observes monitoring consent, and checks listener connection every 30 seconds without polling messages. Pause stops it. Android controls service scheduling; Force stop, system task-manager Stop, reboot and manufacturer battery restrictions can still interrupt protection. Open Sentinel again after an explicit stop; app battery settings are linked from Protection.

The manifest declares FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE with an explicit local notification-protection subtype. Android 14+ uses the specialUse foreground type; earlier supported releases use the standard foreground call. No inbox, internet, overlay, accessibility or battery-exemption permission was added. API 34+ behavior and store review approval have not been physically demonstrated.

For HIGH/REVIEW results, Sentinel first posts its warning and then requests dismissal of exactly the detected source notification. It does not delete a message from SMS, WhatsApp or Gmail. If its alert cannot post, the source notification remains. Benign, unknown, hidden, unselected, summary and ongoing notifications are not dismissed by this path. Original-source references stay in memory and never enter history.

Preview revisions prevent an older queued analysis from dismissing a newer accepted update on the same notification key. Consent/source changes or disconnection invalidate queued work. An identical suspicious repost reuses the current verdict to replace its notification without another scan/history entry. Android can decline cancellation; the warning remains and Protection reports the dismissal failure.

Professional alert copy uses “Potential scam detected” for HIGH and “Suspicious message detected” for REVIEW, names the source, and advises avoiding links, payments and personal-data disclosure until the sender is verified. It links to the local assessment without including raw message text, destinations, sender identity or secrets. Lock-screen public copy is generic. The separate quiet protection notification explains that monitoring remains active.

## Observed verification

- Local engine verification: 42 tests passed, zero failures/errors. App, fixture and instrumentation builds succeeded; lint completed with 13 warnings and no errors.
- Isolated Vivo V2117, Android 13/API 33: final full transcript **OK (23 tests)** in 54.134 seconds. Actual Android notifications verify foreground protection, closing the UI task, suspicious source dismissal only after warning posting, identical suspicious repost replacement without rescanning, benign notification retention, pending-warning recovery and pause stopping the foreground service. Existing scan/import/history/notification regressions pass.
- Installed Sentinel 0.7.1 in place using the existing local signing key, preserving user data/grants. App UI showed Listener Connected and Background protection Running; Android service metadata confirmed isForeground=true, notification ID 4101.
- The user independently confirmed the physical two-phone SMS test after swiping Sentinel out of Recents: the suspicious Google Messages notification disappeared and the Sentinel warning arrived. This is a live SMS/background confirmation on this phone, not evidence of universal delivery or real-world detection accuracy.
- Temporary synthetic app/test/fixture packages were removed. Real sources remained OFF during isolated synthetic instrumentation. Local evidence is ignored: background-final-phone-tests.txt, background-user-ui.xml, build/lint reports, and artifacts/Sentinel-0.7.1.apk.

References: [Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [NotificationListenerService cancellation](https://developer.android.com/reference/android/service/notification/NotificationListenerService).
