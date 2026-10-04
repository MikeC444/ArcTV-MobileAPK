# Changelog

Engineering log for the mobile app. One entry per unit of work, newest last.

## 0 — Created from the Fire TV app

**Status:** Written; CI builds it (see the workflow runs). Not tried on a device.

**Context:** Arc TV only ran on Fire TV / Android TV and the web. This repository carries the same app to Android phones and tablets.

**Changes (all relative to the Fire TV app's `main` at the time):**
- `applicationId` is `com.mangotv.app.mobile` (the code's namespace stays `com.mangotv.app`), so the two apps never replace each other; app name unchanged ("Arc TV").
- Manifest: no Leanback launcher or banner, a touchscreen is required, landscape in both directions (`sensorLandscape`), and configuration changes are handled in place so the player never restarts on rotation.
- `MainActivity`: full screen (system bars hidden, back on an edge swipe), draws behind a camera cut-out, and scales the density so the screen's long side is 960 "TV dp" (clamped 0.7 to 1.4), which keeps every layout as it was designed on any phone or tablet.
- Player: tap shows / hides the controls, double-tap the left / right half skips 10 s, tap or drag on the timeline scrubs (the seek happens when the finger lifts).
- Sign-in goes straight to email + password (no QR step); platform reported to the backend is `android_mobile`.
- Plus checkout opens the payment page in the browser instead of showing a QR code; Add Addon is the paste-a-URL form only (no phone-pairing QR / local server).
- Update checks read this repository's releases; the release workflow publishes one APK named `ArcTV-Mobile.apk`.

**Tests performed:** none by hand. CI compiles it and runs the unit tests it inherited. No Android SDK was available where this was written, so nothing was built locally and nothing was run on a device.

**Issues discovered:** none yet.

**Issues fixed:** none.
