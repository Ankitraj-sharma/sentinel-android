# Stage 5: QR and shared input

Camera QR scanning, picker-selected QR images and Android text/plain sharing feed the existing local engine through an explicit review screen. No destination is automatically opened and receiving content does not analyse it automatically. Users can inspect/edit the complete accepted text and choose Analyse locally.

## Entry paths

- Camera: Scan QR with camera requests CAMERA only when tapped. Bundled ZXing Android Embedded 4.3.0, with ZXing Core 3.5.3, decodes QR codes locally; QR-only format selection, no beep, no barcode image saving, private capture activity and rotation support. Camera denial offers QR image/manual alternatives and app permission settings. Back/cancel keeps existing input.
- Image: Choose QR image invokes Android OpenDocument for PNG/JPEG/WebP. Only the selected content URI is opened; no storage permission and no persistent URI grant. The app does not save the selected image. Encoded input is bounded at 12 MB, source dimensions at 40 million pixels, and decoded longest edge at 2048 pixels through sampling. Missing/unreadable/no-QR/ambiguous images fail explicitly; multiple decoded QR codes require cropping to one. Processing runs off the UI thread. Cancel and clear abandons the pending result; a blocking provider read may continue briefly before returning.
- Share: ACTION_SEND with text/plain and EXTRA_TEXT. The exported activity treats incoming extras as untrusted, accepts only bounded plain text and handles warm/cold launches. Unsupported types/missing/oversized input produce a recoverable error. Streams, HTML, attachments, ACTION_VIEW destinations and background inbox access are not consumed. Rotation does not replay a handled share over edits.

## Payload and privacy boundaries

Imported text is bounded at 20,000 characters; rejected content is never silently truncated or analysed. QR input supports HTTP(S) web destinations only. Payment/Wi-Fi/contact/app commands and control-containing or multiline destinations are not assessed. Existing engine limitations still apply: example brand coverage, no redirects/reputation/page fetches, English rule coverage and no calibrated probability of fraud.

A new import clears previous verdicts and shows its origin. Editing imported content changes origin to Manual input; input/result stay in memory. Cancelled picker/camera operations keep prior input. Clear resets import errors, and Cancel and clear is available during image reading. Persistent history follows Stage 6.

The production manifest has CAMERA and POST_NOTIFICATIONS; no Internet, inbox or broad image/storage permission. Camera hardware is optional so share/manual/image paths remain usable without a camera. The synthetic content provider is packaged only in the instrumentation APK.

## Evidence and references

Independent critic accepted 9/10 after two review/improvement rounds. Observed results are recorded in [Stage 5 review](stage-5-review.md). Library: [ZXing Android Embedded official README](https://github.com/journeyapps/zxing-android-embedded). Platform: [Android receiving shared data](https://developer.android.com/develop/ui/compose/sharing/receive) and [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files).
