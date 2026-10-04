# Release notes

User-facing changes to Arc TV for Android phones and tablets, newest first. This file is the release notes shown in the app's own
update pop-up: whatever sits under a version heading becomes that release's description. Add each change as a bullet under
`## Unreleased` as you make it; publishing a release files them under its version automatically.

## Unreleased

- Sources now rank by how likely they are to play on your phone: plain H.264 first, then HEVC, then 10-bit or HDR. Releases that don't name their codec (4K, Dolby Vision, HDR) are treated as HEVC. Ones your phone can't decode go last and say "May not play on this phone", and a failure now explains itself plainly.
- Fixed: sources with Dolby Digital (AC3), E-AC3, DTS or TrueHD sound played with no audio on many phones. The app now decodes them itself.
- Tap a Continue Watching title and it carries straight on, using the same source you watched before, with no source list.
- Opening a title now shows its artwork and logo with a loading symbol, instead of a blank screen.
- A Next episode button appears in the last minute, and Auto Play Next Episode now really plays the next one.
- Your playback speed is remembered, and the right-hand time shows time left; tap it for the total.
- Settings in the player always has an Audio row, and it tells you when a source has only one track.
- Continue Watching now remembers any amount you watch, and always shows on Home.
- Cancel a monthly or yearly Arc TV Plus subscription right from Settings > Arc TV Plus.
- Sixteen new illustrated profile pictures to choose from.
- Recent searches are now kept separately for each profile.
- Settings > Addons now explains that only addons with a debrid service will play.
- The Trailer button now has the same rounded shape as Play.

## 0.1.8

- Fixed signing in showing "Not found": the app was asking the server for the wrong address.
- The sign-in form now fits the screen, with margins at the sides, and scrolls up so the keyboard doesn't cover it.

## 0.1.7

- A new look made for touch: a bottom bar to move between Home, Movies, TV Shows, Search and My List, and a side bar on tablets.
- Home has a banner you can swipe through, with Play and My List buttons, and rows you scroll with your finger.
- Movies, TV Shows and My List are poster grids that fit your screen, with filters and sorting as chips you tap.
- Search works like any phone app: tap the box and type. Recent searches can be tapped or removed with the X.
- Title pages are laid out for a phone, with one big Play or Resume button, a tappable episode list, cast and similar titles.
- Settings is a simple list on a phone: tap a section to open it, and the arrow takes you back.
- The app turns with your phone. Videos play in landscape, and the player controls fit on a phone screen.
- Press and hold a poster for quick actions.
- The update pop-up shows these notes in larger text that you can swipe through.

## 0.1.0

- Arc TV on your Android phone or tablet: the same library, addons, My List and Continue Watching as your Fire TV.
- Made for touch: tap to show the controls, double-tap either side to skip 10 seconds, and drag the progress bar to scrub.
- Sign in with your email and password, and pay for Arc TV Plus in your browser.
