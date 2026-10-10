# Signed APK distribution

Use the APK attached to GitHub Releases, not Actions debug/test artifacts.
The distribution package is `com.phishshield.sentinel`; version 0.7.3 has version code 10.
It supports Android 8.0+ and includes arm64-v8a, armeabi-v7a, x86 and x86_64 libraries.
Camera hardware is optional. This is not a guarantee of installation on every device:
device policy, storage, installer restrictions and existing package signatures still apply.

## Signing and future updates

The permanent distribution key is stored locally in `.tools/signing/sentinel-release.jks`.
Its passwords and path are in the ignored root `keystore.properties`. Back up both files
privately in an encrypted location: losing the key prevents compatible updates.
Never commit or share these files with APK recipients.

Build with JDK 17 and the configured Android SDK:

```powershell
./gradlew.bat :engine:test :app:assembleRelease :app:lintRelease
```

Release tasks fail without signing configuration or when isolatedValidation is enabled.
Release is not debuggable; shrinking is disabled for the initial distribution build.
Increase versionCode for every published update and preserve the package and key.
Verify the APK with Android Build Tools `apksigner verify --verbose --print-certs`.

Previously installed standard debug packages cannot update to this new key. Do not
uninstall automatically: uninstalling deletes their settings and encrypted history.
The separate `.validation` app can coexist but has its own data and permissions.

## Install and report failures

Download `Sentinel-0.7.3.apk` from the release and save it in Downloads. Open with Files;
allow installation from that source if Android requests it. Enable Sentinel monitoring,
notification access, selected sources and warning notifications separately after installation.
If Android restricts sideloaded notification access, review App info's Restricted settings
option only after verifying the downloaded app's source.

Test both a fresh Files-app install and a subsequent same-key update on different devices.
For failures, record model, Android version, existing Sentinel version and the exact installer
error. ADB success alone does not validate the browser/Files/WhatsApp installation path.

## Previous diagnostic evidence

On iQOO Neo 10R (I2221), Android 16, the 0.7.2 validation debug APK installed and launched
through ADB. The user subsequently confirmed installation/update through Files.
The previously shared WhatsApp APK had the identical SHA-256 to the verified local build:
`596de7a562f0dbb69dae4502605ae03ddd84a1acde48114c522621a063cc01cd`.
The earlier generic installer failure was not reproduced or diagnosed; do not claim that
release signing fixes its unidentified cause.

Reference: https://developer.android.com/studio/publish/app-signing
