# Stage 1 review

## Scope

Android foundation, English onboarding, navigation, persistent preferences, and honest inactive feature states. Detection is outside this stage.

## Critic review 1

Independent critic: provisional source rating **8.0/10**. Build and runtime evidence were pending. This is not a verified final rating.

Changes made in improvement round 1:

- Preference rows are full-width accessible switches with merged labels and summaries.
- Settings writes run in a ViewModel scope to survive Activity recreation.
- Successful saves clear previous save errors.
- Onboarding shows save errors and lets the user retry.
- Scan and History describe feature availability without developer stage numbers.

## Verification status

Build, lint, APK installation, launch and device interaction verification are in progress. Record actual results below before declaring this stage complete.

## Device acceptance procedure

1. Install the debug APK on a clean device/app data state.
2. Confirm welcome copy, continue, and inspect inactive protection.
3. Navigate every tab and use Android Back.
4. Toggle appearance, source selections and history preference.
5. Force-stop and relaunch; verify values and onboarding persistence.
6. Recreate the Activity; verify navigation and saved settings.
7. Inspect light/dark modes and enlarged-text layout.
8. Confirm no message, internet, notification or camera permissions in the merged manifest.
9. Run `connectedDebugAndroidTest`; inspect output rather than assuming a pass.

Do not record synthetic results as live runtime evidence.
