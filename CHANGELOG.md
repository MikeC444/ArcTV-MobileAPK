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

## 1 — Touch-first phone and tablet interface

**Status:** Written; CI builds it. Not tried on a device.

**Context:** Version 0 carried the TV interface over, scaled to fit. This replaces that with screens designed for touch and for phone / tablet sizes.

**Changes:**
- The TV density scaling (`TvLayoutScale`) is gone. `MobileMetrics` holds the window width (Compact under 600 dp, Medium under 840 dp, Expanded above) and `MangoDimens` reads it, so margins and poster sizes follow the window and re-lay-out on rotation.
- Orientation follows the device (`fullUser`); the player forces landscape and hides the system bars, and keeps clear of a camera cut-out. Elsewhere the app stays clear of the status and gesture bars.
- Navigation: a Material bottom bar (Home, Movies, TV Shows, Search, My List) on Compact, a side rail with Settings on wider windows. `TopNavBar` is now a slim bar with the logo, profile picture (Plus) and a Settings gear, or "Sign in" for a guest.
- New touch screens: Home (swipeable pager hero, sideways rows), the Movies / TV Shows / My List grid (`MobileBrowseGrid`), Search (plain text field, tap or remove recents), and the title page (`MobileDetailContent`). Posters (`MobilePoster`) tap to open and long-press for the quick-actions menu, with a haptic buzz.
- Settings is a category list that opens a full-screen page on Compact (two panes on wider windows); sign-in start, "Who's watching?", the source picker, the Plus plans and Add Addon are adapted to narrow screens; the player's controls split onto two lines below Expanded.
- The loading placeholders follow the same column rule as the real grid.

**Tests performed:** none by hand. CI compiles it.

**Issues discovered:** the old Search field only accepted typing after a remote's OK press, which would not have worked by touch (replaced).

**Issues fixed:** the above.

## 2 — Sign-in fixes

**Status:** Written; CI builds it. Not tried on a device.

**Context:** On a phone, signing in answered "Not found" and the form touched the screen edges.

**Changes:**
- `API_BASE_URL` is trimmed and stripped of trailing slashes at build time. A secret saved as `https://host/` made every request go to `https://host//auth/login`, which the backend answers with its 404 "Not found" (a wrong password is "Invalid email or password").
- A 404 from sign-in is now shown as "Couldn't reach the Arc TV server" rather than the raw text.
- The password sign-in screen has side margins, scrolls, and lifts above the keyboard.

**Tests performed:** none by hand. CI compiles it.

**Issues discovered:** the above; the release secret had a trailing slash.

**Issues fixed:** the above.

## 3 — Don't recommend a video the phone cannot decode (0.1.9)

**Status:** Written; CI compiles it and runs the new unit tests. Not tried on a device.

**Context:** 4K HEVC sources failed on the emulator with "Decoder failed: c2.goldfish.hevc.decoder ... NO_EXCEEDS_CAPABILITIES". "Quality" order and the
Recommended source put the highest resolution first, which on a phone is often one it cannot play.

**Changes:** `DeviceVideoSupport` asks `MediaCodecList` whether a decoder exists for the source's codec at its resolution (unknown = playable); `playRank` puts
sources the device can decode ahead of ones it cannot in the Quality order (after "starts at once", before resolution), so Recommended is one that plays;
Select a Source marks the others "May not play on this phone"; the player's video-decoder error now starts with a plain-English line. Fire TV is unchanged.

**Tests performed:** unit tests for the ordering, codec mapping and the unknown-is-playable rule; none on a device.
