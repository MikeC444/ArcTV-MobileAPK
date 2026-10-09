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

## 9 — Parity with the Fire TV app, phase 2: the VLC player with touch controls

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** On Fire TV, VLC's engine (LibVLC) is the default player because it plays sources the built-in player has no decoder for (4K HEVC, Dolby Vision, DTS-HD
...). Phase 2 of the parity work brings it to the phone.

**Changes:** LibVLC 3.6.5 added (`libvlc-all`, arm64-v8a and armeabi-v7a only, native libraries kept compressed in the APK as on Fire TV). `VlcPlaybackContent`
plays a source with VLC: loading screen until the first picture, resume from the saved position (`:start-time`), Continue Watching reports (first after 4 s, then
every 15 s, on pause and on leaving), the account's audio and subtitle language preferences, the Next episode button and Up next card, and touch controls: tap to show
or hide, double-tap the left or right half for 10 s back or forward, drag the bar to seek, Audio / Subtitles / Speed lists. If VLC cannot play a source, a card
offers the built-in player. `PreferredPlayer` and the per-title / default player storage were added to `DevicePlayerPrefs` (VLC is the default; the Settings page and
the choose-player card come in phase 3); `PlayerScreen` starts VLC for a source with a direct link on a supported chip, otherwise the built-in player.

**Tests performed:** a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`), so it is only compile-checked by CI on the branch.

## 10 — Parity with the Fire TV app, phase 3: default player, per-title player, Other Players and hand-off tracking

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** Phase 3 of the parity work (see entry 8). Fire TV lets people choose the player per title, offers "Other Players" when a source won't play, and reports
hand-offs to the backend for the developer panel's Other players section.

**Changes:** Settings > Player (VLC or built-in as the default; stored with the other device-only player settings). `PlayerChoiceCard` ("How do you want to play
this?": built-in, VLC engine, another app; the choice is remembered for the title and used the next time it opens). It opens from the "Unable to play" card's
Other Players button, the built-in player's Advanced settings ("Choose Player") and a Player chip in the VLC controls; the VLC error card now offers it too.
Another app is launched with a view intent (manifest `<queries>` added so installed players are visible). `ExternalPlayerRepository`, `PlayerEventsApiClient` and the
event DTO (ported unchanged) post `/user/player-events/external` for each hand-off, with `engine` "external" or "vlc" and trigger "error" or "button"; fire-and-forget,
never sending the stream address. `PlayerViewModel.recordExternalPlayer` builds the report.

**Tests performed:** a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`); CI compiles it on the branch. Not tried on a device.

## 11 — Remove the interface sounds

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** The phone app inherited the Fire TV app's navigation, click and back tones; on a phone they are unwanted.

**Changes:** `UiSoundPlayer`'s `playNav`, `playClick` and `playBack` are now no-ops (no sound pool is loaded), so every button, card and menu is silent without touching
each call site. The Settings > Sounds page (a volume for those tones) is removed. The sound files and the stored volume are left in place, unused.

**Tests performed:** a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`); CI compiles it on the branch. Not tried on a device.

## 12 — Select a Source opens on the 1080p filter

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** 4K sources (HEVC, often 10-bit / HDR) are usually software-decoded on a phone and stutter, while 1080p plays on the hardware decoder. Phone app only; Fire TV keeps All Sources.

**Changes:** the Select a Source filter starts on 1080p when the title has any 1080p source, otherwise on All Sources, and stays wherever the person taps once they choose. The Recommended row still always sits on top.

**Tests performed:** a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`); CI compiles it on the branch. Not tried on a device.

## 13 — Parity with the Fire TV app, phase 4: the Audio filter on Select a Source

**Status:** Written; not built here. To be compiled (with the new unit tests run) by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** Phase 4 of the parity work (see entry 8). The Fire TV app can filter the source list by sound type; a release name says its layout ("DDP5.1", "DTS-HD.MA.7.1", "Atmos").

**Changes:** `Stream` gets `audioChannels` and `audioAtmos`, read from the release text by `detectAudioChannels` / `detectAtmos` (ported unchanged). A new Audio drop-down pill
in the filter bar (the Fire TV `DropdownPicker` and `SourceFilterBar`, which only gained the compact / highlighted looks) lists "All audio" plus each kind this title's
sources have (Stereo, 5.1, 7.1, Atmos) and "Not listed", each with a count, and is hidden when no source names its audio. Unlike the Fire TV version there is no "Match my
speakers" choice (the speaker setup is a TV setting the phone app doesn't have), so it opens on All. The Recommended row follows what is listed.

**Tests performed:** unit tests added for the name parsing, the choices, the filtering and the labels (run by CI); a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`). Not tried on a device.

## 14 — Parity with the Fire TV app, phase 5: Episodes panel and in-player episode switching

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** Phase 5 of the parity work (see entry 8). The phone app started the next episode by navigating back through the source screen; the Fire TV app switches inside the
player, with an Episodes selector.

**Changes:** `PlayerViewModel.playEpisode` finds the episode's sources, uses the one it was last watched on (else the recommended one) and swaps the player to it, via a new
`PlayerScreenUiState.Switching` that shows the loading screen (backdrop and logo) meanwhile; with no source found the error card offers the source list. The season and
episode being played are now state in the view model, and progress reports are filed under the episode actually on screen (`activeReady` / `activeSeason` / `activeEpisodeNumber`,
which only move on when the new player is ready, so the old player's last report still lands on its own episode). `EpisodePanel` (ported from the Fire TV app, made
phone-width with tap-outside-to-close) lists each season's episodes with thumbnails; it opens from an Episodes button in the built-in player's icon row (replacing Next
episode there) and an Episodes chip in the VLC controls. The Next episode button and the Up next card now start the episode in place too.

**Tests performed:** a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`); CI compiles it on the branch. Not tried on a device.

## 15 — Parity with the Fire TV app, phase 6: the redesigned update pop-up

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** Phase 6 of the parity work (see entry 8). The Fire TV pop-up was redesigned (the page behind only dims instead of going near-black, a fade-in, the version as a pill, a divider and
a dotted list of notes); the phone still had the older, plainer card.

**Changes:** `UpdatePopup` is now the Fire TV one, kept for the phone: the dialog's own dimming is turned off so the 55% backdrop alone decides how dark it is, with the backdrop and card fading
in together; the version is a pill in the header with "Download size" under the title; a divider; notes as accent-dotted lines (the notes text already comes as "• " lines from
`plainNotes`). Phone adjustments: the card fills the width minus a 16 dp margin up to 560 dp held upright (480 dp sideways), compact buttons, the notes box is 240 dp tall held upright and
170 dp sideways, and the hint reads "swipe the notes to see more".

**Tests performed:** a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`); CI compiles it on the branch. Not tried on a device.

## 16 — The Select a Source loading skeleton matches the real layout

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** After tapping Play the placeholder boxes did not look like the screen that replaced them. `SourcesLoadingSkeleton` always drew the sideways two-pane layout (a title panel on the left),
while `SourcesContent` on an upright phone has no title panel: a back arrow and the title sit over a full-width list. The skeleton also had one wide box where the real screen has a row of filter pills.

**Changes:** the skeleton follows the same split as `SourcesContent` (`MobileMetrics.isCompact`): upright, no left panel, a back arrow with a title placeholder, the same margins and a longer list of rows; both
orientations now show the row of filter pills (Audio, All Sources, 4K, 1080p, 720p, Other) in place of the single box.

**Tests performed:** a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`); CI compiles it on the branch. Not tried on a device.

## 17 — Recommended follows the filter (best 1080p first on the 1080p filter)

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** With Select a Source opening on 1080p (entry 12), the Recommended row at the top was still picked from every source, so a cached 4K H.264 release sat first above a list of 1080p ones.

**Changes:** the Recommended source is now `recommendedStreamId` of what the resolution filter shows (falling back to everything listed when the filter shows nothing), so on the 1080p filter it is the best 1080p source (cached first, playable on this phone,
then the likeliest codec, then seeders), and picking All Sources or 4K recommends from those. Entry 12's other behaviour is unchanged. The next-episode auto-pick in the view model still uses every source.

**Tests performed:** a manual re-read of the diff. No Gradle build is possible in this sandbox (no route to `dl.google.com`); CI compiles it on the branch. Not tried on a device.

## 18 — Plus 5-day free trial, and a payment page that fits a phone

**Status:** Written; not built here. To be compiled by the "Build debug APK" workflow on the branch. Not tried on a device.

**Context:** the backend now offers a 5-day free trial on a first Monthly or Yearly Plus subscription (`GET /user/plus` and the checkout answer carry `trialDays`), and the Fire TV app shows it. The phone app's Plus screens are the same code as the Fire TV's.

**Changes:** ported from the Fire TV app (same files): `trialDays` is read from the Plus status and the checkout answer (0 from an older backend); the Plus tab shows "5 days free" on the Monthly and Yearly cards, a "Start 5-day free trial" button, a line in the steps and a footer note; the pay-on-your-phone page shows "Free for 5 days", Due today "Free" and a trial billing note (Stripe reports £0.00 for the first charge during a trial). The plan panel on that page scrolls, so the Change plan button is no longer squeezed and cut off. New unit tests for the helpers in `PlusPriceTest`.

**Tests performed:** a manual re-read of the diff; the unchanged files are identical to the Fire TV app, where the same code compiled and its `PlusPriceTest` passed. No Gradle build is possible in this sandbox (no route to `dl.google.com`); CI compiles it on the branch. Not tried on a device.

**Payment page layout (same entry, user screenshot):** on an upright phone `PlusCheckoutPage` squeezed the plan panel and the pay panel side by side, so the Open payment page button was reduced to a few letters, the headline said "on your phone" (the person is on it), and the Change plan button was cut off below. Now, when `MobileMetrics.isCompact` (and not on the thank-you state), the page scrolls and stacks the panels full width: shorter step labels (Plan, Pay, Done), a smaller headline "Finish your payment" with wording about opening the payment page, a full-width Open payment page button and a full-width Change plan button; the plan panel no longer takes focus there (it would scroll the page away). Tablets and sideways keep the two-panel layout. Not built here (the Build debug APK run on the branch compiles it) and not tried on a device.

## 19 — Built-in torrent streaming, ported from the Fire TV app

**Status:** Built here: `:app:compileDebugKotlin`, `:app:testDebugUnitTest` (251 tests, 0 failures) and `:app:assembleDebug` all succeed. CI compiles it on the branch too. Not tried on a phone.

**Context:** the Fire TV app plays magnet links, .torrent files and addon info-hash sources through an embedded libtorrent engine (its changelog, Post-Milestone-90). The phone app only said torrents were not supported.

**Changes (same engine and behaviour as the Fire TV app; its files copied unchanged):**
- `data/torrent/*` (magnet parsing, video-file choice, piece window and deadlines, the engine, the loopback HTTP server both players read from, subtitles, presets, storage planning) and `data/torrent/platform/*` (`TorrentStreamManager`, `CustomTorrentRepository`), with their unit tests (the real-swarm test, which needs a desktop native library, is not included).
- Addon sources with an info hash or magnet are mapped to torrent sources (`StremioMapper`, `StremioModels`, `Stream`); the player hosts them (`TorrentSourceHost`): loading screen with live progress, retry / choose another source on failure, and the torrent and its temporary files are removed when the player closes, the source changes or the next episode starts. Both players (built-in and VLC) just play the engine's local address; ExoPlayer waits up to 2 minutes for a slow piece.
- Select a Source: a "+ Torrent" pill and the Add a torrent sheet; added torrents are remembered per title / episode and listed first.
- Settings > Player: Torrent Buffer and Torrent Storage Limit. Addons note reworded: "Torrent sources play inside Arc TV".
- `libtorrent4j` 2.1.0-39 (MIT) with the arm64-v8a and armeabi-v7a native libraries only, matching the app's ABI filters (the APK grows by about 30 MB for them).

**Phone-only (not in the Fire TV app):**
- Add a torrent is a bottom sheet that rises above the keyboard, with a Paste from clipboard button and a Choose file button (the system file picker), full-width touch targets, and tap-outside to close.
- Mobile data: a torrent downloads and uploads a lot, so on mobile data or any metered connection (`ConnectivityManager.isActiveNetworkMetered`) the player first asks "You're on mobile data" with Use mobile data this once / Choose a different source. Settings > Player > Torrents on Mobile Data (Ask first, the default / Always allow) keeps the choice.

**Also ported from the Fire TV app (same code):** the one-off "Addons now support torrents" pop-up on Home for accounts that were signed in when this version first opened, and its admin reporting (`TorrentIntroStore`, `FeatureIntroApiClient`: once per user, the click is sent to the server for the developer panel and retried until it gets through); Settings > version and Check for updates (`UpdateCheckRow`, `UpdateViewModel.checkNow`): a footer card under the category list on an upright phone, pinned under the side panel on wider windows. Phone adjustments: the pop-up has side margins and a full-width Got it button and its last point mentions the mobile-data check; the Addons note's "Click here for help" opens the web guide (https://web.arctv.org/guides/debrid) in the browser instead of showing a QR code.

**Phone-only, opening torrents from other apps:** `MainActivity` now takes a magnet link (VIEW, `magnet:`), a link to a .torrent file (VIEW, http/https ending in .torrent), a .torrent file (VIEW or SEND of `application/x-bittorrent`, from a file manager or a download) and shared text containing either (SEND `text/plain`; the first magnet link or .torrent address in it is used). A file is copied into the app's cache at once (`readIncomingTorrent`) before the sender's read permission can lapse, and refused with a toast if it is not a real .torrent file. The result waits in `IncomingTorrentInbox`; once the app is past the sign-in screens and the profile picker, `MangoNavHost` opens "Play this torrent" (`TorrentOpenScreen`): what was received, a search box (the Search screen's own search), and for a show a season / episode picker. Choosing attaches it to that title as a "My torrent" source (`CustomTorrentRepository`, so it also shows on Select a Source) and goes straight to the player. A guest is told to sign in. New unit tests (`IncomingTorrentTest`: finding a link in shared text, labels).

**Tests performed:** as in Status. The unit tests are the Fire TV app's engine tests (ranges, piece window, storage plan, magnet parsing, file choice, subtitles, local server). The torrent engine was exercised against a real libtorrent swarm in the Fire TV app's desktop test; that was not repeated here.

**Issues discovered:** none new.

## 20 — Plus parity with the Fire TV app: Smart source picking, Your stats, Plus settings, Cinemeta for accounts, startup logo

**Status:** Done in code; compiles and the unit tests pass; not run on a phone.

**Context:** the Fire TV app (0.3.0 / 0.3.1) had Plus features the phone app lacked, and the phone's Plus list promised Smart source picking without it existing.

**Changes:**
- Smart source picking: a per-device switch in a new **Plus settings** tab (locked without Plus). When on, the Sources screen waits for the addons (a short grace once a sure pick is in), then plays the best source that surely plays here (`isSurePick`: starts at once, this phone can decode it, has a link); otherwise the list shows with a note.
- **Your stats** Settings tab (locked without Plus, with a Plus tag; shown as a locked row in the phone's Settings list), built from `GET /user/history` by `WatchStats.kt` / `WatchStatsRepository`, same maths as the Fire TV and web apps.
- Accounts whose addons offer no catalogue (none at all, or only stream addons such as Torrentio) get Cinemeta added and saved to the account, once per account on each device (`addDefaultIfNoCatalog`).
- A black screen with only the Arc TV logo is held for 3 seconds at a cold start, then fades (`StartupSplash`).
- The Plus perk list now lists Smart source picking and Your stats as included; the Plus pop-up no longer calls Smart source picking "on the way"; the free-features note names Blocked Genres.

**Tests performed:** `:app:compileDebugKotlin` and `:app:testDebugUnitTest` pass (including `WatchStatsTest` and the updated `PlusPlansTest`). NOT run on a phone, and no screenshot was taken: the screens are copies of the Fire TV ones, so how they lay out on a phone (and the Stats panel on a narrow screen) is unverified.

**Issues discovered:** the phone's Plus list claimed Smart source picking while nothing implemented it.

**Issues fixed:** that claim now matches the app.

## 21 — Home rows match the web and Fire TV apps: Popular, New, Top rated, nine genres, a different mix each day

**Status:** Done in code; compiles and the unit tests pass; not run on a phone.

**Context:** the web app's new Home layout (docs/PARITY.md in the web repo) was missing on the phone, and the Fire TV app now has it (Post-Milestone-106 there). This is the same change, applied unchanged (the two apps share this code).

**Changes:** for Cinemeta (and any addon with `top`, `year` and `imdbRating` catalogues) Home is Popular, New (this year) and Top rated, then Action, Comedy, Drama, Thriller, Horror, Sci-Fi, Crime, Animation and Documentary; each ranking is read from a different page chosen by account + day and gently reshuffled (`HomeVariety.kt`); titles that are watched, in My List or rated are left out of the catalogue rows; New and Top rated go above rows the person already chose until they place them in Settings (`HomeRowPreferences.applyOrder`). `StremioAddonClient.fetchCatalog` is `open` for tests.

**Tests performed:** `HomeVarietyTest` (11, same as the Fire TV app's) and the whole unit suite pass; `:app:compileDebugKotlin` succeeds. Not run on a phone: how Home looks with the new rows is unverified.

**Issues discovered:** none.

**Issues fixed:** none.

## 22 — The Plus free trial is for new yearly subscribers only

**Status:** Done on branch `claude/trial-yearly-only`, not merged.

**Context:** the 5-day Plus free trial was offered on the monthly and yearly plans. It is now yearly only, in the server, the web app and the Fire TV app (Post-Milestone-120 there); this is the same change on the phone.

**Changes:** only the yearly plan card shows the trial (button, "5 days free" pill, checkout page); monthly shows its normal wording; the "How to subscribe" and footer text say the trial is for yearly. The server decides who gets the trial, and now only gives it on the yearly plan.

**Tests performed:** `:app:testDebugUnitTest` (including the updated `PlusPriceTest`) and `:app:compileDebugKotlin` pass offline. NOT run on a phone, and no screenshot was taken.

**Issues discovered:** none.

**Issues fixed:** none.

## 23 — Web and Fire TV changes carried over: poster menu, Picked for you with shows, Recommendations tab, Plus popups

**Status:** Done on branch `claude/phone-web-parity`, not merged.

**Context:** eight web changes (docs/PARITY.md in the web repo) were on the Fire TV app and missing on the phone. The phone started as a copy of the Fire TV app, so most of the code is the Fire TV's files carried over (Fire TV Post-Milestones 110 to 119); only the phone's own layouts needed adapting.

**Changes:**
- Poster menu: big Play or Resume, a two-by-two grid of My List, Watched, Like and Not for me (a teal tick and "In My List" / "Watched" show what is set, pressing again undoes it, and the menu stays open), then View details and Choose source rows; the title's backdrop with its logo (or title text) on top, or a small poster and the title if there is no backdrop. Sized to fit a phone's width ("Mark watched" instead of "Mark as watched" so it fits half the width). Tapping the dimmed area outside the card closes the menu (a tap on the card itself does not).
- Picked for you takes TV shows; Like and Not for me appear on shows (movie-only before).
- Remove from Picked for you hides the title for 5 days and syncs across devices; Remove from Continue Watching no longer marks the title watched (needs the server's new endpoints, already in the Fire TV repo's server).
- Settings > Recommendations (Plus only): the Liked and Not for me titles, what shapes your picks, how it works, Reset preferences.
- Plus member welcome popup (once ever on the device) and the Plus invitation card restyled the same size; invitation snoozes 5 days.
- Test setup: Robolectric and Compose test dependencies, and screenshot options, added to the build (as in the Fire TV app).

**Tests performed:** `:app:testDebugUnitTest` (including the carried-over menu, Recommendations, Plus popup and removal tests, with screenshot tests redrawn at phone size, and a test that a tap outside the card closes the menu while a tap on it does not) and `:app:assembleDebug` pass offline. Pictures of the menu, Recommendations tab and both Plus popups were drawn at 411x891 dp on the desktop and looked at. NOT done: any of this on a phone. The Fire TV's D-pad focus code came along inside shared files (menu focus trap, restore) and is unused on a touch screen. Touch behaviour (long-press to open the menu, tap targets, the Recommendations tab inside the phone's Settings list) has not been tried.

**Issues discovered:** the Recommendations tab's text blocks are focus targets for a remote; on a phone they are just cards.

**Issues fixed:** none.
