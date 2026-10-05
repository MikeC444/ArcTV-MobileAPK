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

## 3 — Catch up with the Fire TV app (0.1.9)

**Status:** Written; CI compiles it. Not tried on a device.

**Context:** This app was copied from the Fire TV app before its web-parity work, so it lacked those changes.

**Changes (ported from ArcTV-AndroidTV `ba0cc5c..dc997c1`, backend and workflows excluded):**
- Continue Watching: saves from the first seconds and on leaving, always shows on Home, and a press resumes on the remembered source (`LastSourceRepository`
  also keeps addon / release name / hash so a re-issued id is found again); the resume question is gone.
- Player: loading screen, Next episode offer and Up next, remembered speed, time-left toggle, Audio row always present, remembered control focus.
- Cancel Plus subscription, 16 new profile pictures (and the nearest-picture mapping for old ids), per-profile recent searches, debrid note on Addons,
  pill Trailer button.
- Mobile wiring: `MobileHomeContent` takes `onResume`; `PlayerBottomControls` (two-line layout) got the remembered-focus and time-left pieces.
- Not ported (remote-control only): 10-second hold-to-scrub steps and the profile editor's keyboard / scroll behaviour.

**Tests performed:** none by hand. CI builds and runs the unit tests (including the new `PlayerLogicTest` and `MatchLastSourceTest`).

## 4 — Audio for formats a phone cannot decode (0.1.9)

**Status:** Written; CI compiles it. Not tried on a device.

**Context:** A source with AC3 audio (Source Info: "Audio AC3") played with no sound on a phone. A Fire TV passes those formats through to the TV; a phone has
no decoder for them and ExoPlayer then plays the picture silently, without an error.

**Changes:** media3 1.4.1 -> 1.5.0 and the Jellyfin `media3-ffmpeg-decoder` (1.5.0+1, the first build matching a media3 this app can use); `compileSdk` 35 (the
decoder requires it; `targetSdk` stays 34, AGP 8.5.2 warning suppressed); `DefaultRenderersFactory` extension mode OFF -> ON (phone decoder first, FFmpeg as
fallback); native libraries limited to arm64-v8a and armeabi-v7a. The Fire TV app is unchanged (it passes audio through).

**Tests performed:** none by hand.

## 5 — Don't recommend a video the phone cannot decode (0.1.9)

**Status:** Written; CI compiles it and runs the new unit tests. Not tried on a device.

**Context:** 4K HEVC sources failed on the emulator with "Decoder failed: c2.goldfish.hevc.decoder ... NO_EXCEEDS_CAPABILITIES". "Quality" order and the
Recommended source put the highest resolution first, which on a phone is often one it cannot play.

**Changes:** `DeviceVideoSupport` asks `MediaCodecList` whether a decoder exists for the source's codec at its resolution (unknown = playable); `playRank` puts
sources the device can decode ahead of ones it cannot in the Quality order (after "starts at once", before resolution), so Recommended is one that plays;
Select a Source marks the others "May not play on this phone"; the player's video-decoder error now starts with a plain-English line. Fire TV is unchanged.

**Tests performed:** unit tests for the ordering, codec mapping and the unknown-is-playable rule; none on a device.

## 6 — Rank sources by how likely they are to play on this phone (0.1.9)

**Status:** Written; CI compiles it and runs the new unit tests. Not tried on a device.

**Changes:** the Quality order is now: starts at once, then playable on this device (`MediaCodecList`, now also checking 10-bit for HDR / Dolby Vision / "10bit"
releases), then `likelihoodTier` (0 plain H.264, 1 HEVC / AV1 / VP9, 2 10-bit / HDR), then resolution, then seeders. Recommended follows the same order.

**Tests performed:** unit tests for the ranking, the 10-bit rule and the keyword spotting; none on a device.

## 7 — Infer the codec of releases that don't name it (0.1.9)

**Status:** Written; CI compiles it and runs the new unit tests. Not tried on a device.

**Context:** In Select a Source the Recommended pick was a 4K "...2160p.DV5" release with no codec shown: an unknown codec was assumed playable, so it beat the
4K HEVC sources the device is known not to decode.

**Changes:** `DeviceVideoSupport.effectiveMime` uses the codec text, else x265 / H.265 / HEVC / x264 / H.264 / AV1 in the release name, else HEVC for a Dolby Vision /
HDR / 10-bit or 4K release. Only a source with no clue at all is assumed playable.

**Tests performed:** unit tests; none on a device.

## 8 — Parity with the Fire TV app, phase 1: preferred audio language

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** The Fire TV and web apps gained a synced preferred audio language (`defaultAudioLanguage` in `/user/settings`, backend migration 0022). This is the
first of the phases bringing the phone app level (audio language, then the VLC player, default player and other-app hand-off, source Audio filter, Episodes panel,
dimmed update pop-up).

**Changes:** `PlayerPreferences.defaultAudioLanguage`, set from a new Settings > Audio page (same language list as Subtitles, "Automatic" for none) and given to
ExoPlayer as the preferred audio language; sent in the settings push and applied from pulls, where a server that doesn't send it (`AUDIO_LANGUAGE_ABSENT`) leaves the
local choice alone. `LanguageOptionRow` is now shared by both pages.

**Tests performed:** a manual re-read of the diff only. No Gradle build is possible in this sandbox (no route to `dl.google.com`), so nothing was compiled or run.
