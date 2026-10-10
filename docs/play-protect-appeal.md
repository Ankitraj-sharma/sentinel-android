# Play Protect installation block: review and appeal draft

## Observed issue

The user supplied a screenshot showing Google Play Protect's "App blocked to protect
your device" warning and its sensitive-data/financial-fraud explanation during installation.
Google documents this exact warning for Internet-sideloaded apps declaring notification
listener, SMS or accessibility access in supported markets. Sentinel declares a notification
listener service for automatic analysis. This is the likely applicable protection; Google
has not provided an app-specific classification result to us.

Permanent signing addresses update identity, not this distribution restriction.
Do not promise that renamed APKs, changed certificates or a higher version resolve it.
Do not remove notification access from the full product: that disables automatic detection.
Play Protect should remain enabled while investigating.

Sources:
- https://developers.google.com/android/play-protect/warning-dev-guidance
- https://support.google.com/googleplay/android-developer/contact/protectappeals

## Verified release identifiers

- Version: 0.7.3; version code: 10
- Package: `com.phishshield.sentinel`
- APK SHA-256: `5eeb8a604139addd7c92e99c02e53f89c5a13616d03bdac30dcdb79da48d3cd5`
- Certificate SHA-256: `f41af53ce285d43342bf38456d8e05954d501d15eeb3ea8e78cc9f7e7abac0f8`
- APK: https://github.com/ius-sharma/sentinel-android/releases/download/v0.7.3/Sentinel-0.7.3.apk
- Source: https://github.com/ius-sharma/sentinel-android

Inspect the blocked downloaded APK's hash before submitting; the screenshot alone does
not identify its exact version. The appeal must refer to the actual affected APK.

## Permission review

The release manifest declares CAMERA, POST_NOTIFICATIONS, FOREGROUND_SERVICE,
FOREGROUND_SERVICE_SPECIAL_USE and AndroidX's signature-level dynamic receiver permission.
The notification listener service is protected by BIND_NOTIFICATION_LISTENER_SERVICE.
It does not declare INTERNET, READ_SMS, RECEIVE_SMS, accessibility, overlays or broad storage.
Manual/QR/shared-text analysis does not require notification-listener access.

Monitoring requires onboarding, the app's monitoring setting, OS notification access and
selected sources. Notification text is processed locally. History is opt-in and stores
encrypted limited assessment metadata, not message bodies, OTPs, URLs or images.
Warning replacement cancels the source notification only after posting Sentinel's alert;
the source message is not deleted. Review prominent in-app consent for this cancellation
before submitting an appeal; the current Protection screen describes this behavior.

## English additional-information draft

Please review the installation restriction affecting Sentinel, an on-device phishing and
scam detection application. The affected release identifiers and source/download links
are provided above. The warning displayed is "App blocked to protect your device" with
the sensitive-data and financial-fraud explanation.

Notification listener access supports the application's core, user-enabled function:
analysing newly delivered readable previews from selected WhatsApp, Gmail and default SMS
sources and displaying explained scam warnings. It does not provide complete inbox or chat
access. Monitoring is disabled until the user completes setup and enables monitoring and
Android notification access. Users can choose sources and pause monitoring.

Analysis runs on-device; this APK does not request Internet, SMS inbox or accessibility
permissions. Opt-in history stores encrypted limited assessment metadata locally, with
deletion controls. Message bodies, OTPs, URLs and images are not saved in that history.
When monitoring is enabled, the app displays an ongoing protection notification. After
a warning is successfully posted, a detected source notification can be replaced; the
underlying message remains in its source app. This behavior is described in the app.

Please review the provided APK and source for this legitimate notification-analysis use
case and reconsider the installation restriction if appropriate.

## Submission requirements and remaining work

The official form asks for contact email, package name, APK SHA-256 of the file uploaded
to VirusTotal and supporting information. VirusTotal upload and submission are NOT done.
Obtain the owner's contact email and explicit authorization before uploading the public
APK to VirusTotal or submitting the appeal. Do not upload signing keys or private data.
Google's form states that decisions are final and no response is provided; acceptance and
timing are not guaranteed.

For broader distribution, assess Google Play testing/publication with the owner's developer
account, required disclosures and privacy policy. Store review is separate from this appeal.
Until an approved distribution route is established, the sideload block remains unresolved.
