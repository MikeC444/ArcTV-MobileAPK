<h1 align="center">Arc TV for Android phones and tablets</h1>

The Arc TV app for **Android phones and tablets**, with the same account, addons, My List and Continue Watching as the
[Fire TV app](https://github.com/MikeC444/ArcTV-AndroidTV) and the web app at [web.arctv.org](https://web.arctv.org).

## Install

1. On your phone or tablet, open **[the latest release](https://github.com/MikeC444/ArcTV-MobileAPK/releases/latest)** and download **ArcTV-Mobile.apk**
   (direct link: `https://github.com/MikeC444/ArcTV-MobileAPK/releases/latest/download/ArcTV-Mobile.apk`).
2. Open the downloaded file. Android will ask you to allow installing apps from your browser (or from Files): allow it for this one install.
3. Open **Arc TV**, sign in with your Arc TV email and password (or create an account), and your list and addons are there.

The app tells you when a new version is out and what changed, and installs it for you (Android asks you to confirm each update).

Needs Android 6.0 or newer. The app runs in landscape (turn the phone sideways).

## What's different from the Fire TV app

This is the Fire TV app's code, adapted for touch (see `CHANGELOG.md`):

* touch controls in the player (tap to show them, double-tap to skip 10 s, drag the progress bar), full-screen with the system bars tucked away;
* the layouts keep their TV proportions on any screen (the whole UI is scaled so a phone shows what a TV does);
* sign in with email and password straight away (no QR code: the phone is the device you would scan with);
* paying for Arc TV Plus opens Stripe's page in your browser; adding an addon is by pasting its address;
* its own app id (`com.mangotv.app.mobile`), so it never replaces the Fire TV app, and its own update channel (this repository's releases).

## For the developer

* Build: `./gradlew assembleDebug` (needs the Android SDK). CI builds a debug APK on every push to `main` and `claude/**` branches
  (`.github/workflows/build-apk.yml`; download it from the run's artifacts).
* Release: push a tag like `v0.1.0` (or run **Build and publish release APK** by hand). It signs the APK and attaches `ArcTV-Mobile.apk` to a
  GitHub release, with the notes from `RELEASE_NOTES.md`. It needs these **repository secrets** (Settings → Secrets and variables → Actions):
  `API_BASE_URL` (the backend's https address, same as the Fire TV app), `RELEASE_KEYSTORE_BASE64` (the keystore file, base64), `RELEASE_KEYSTORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Using the same keystore as the Fire TV app is fine.
* The backend lives in the Fire TV repository (`server/`). This app sends `X-ArcTV-App-Version` and signs in with platform `android_mobile`, so it shows
  in the developer panel with its version.
* The Fire TV app and this one share most code. A fix made in one usually belongs in the other; `docs/FIRESTICK_PARITY.md` in the web repository tracks what the Fire TV app is missing.
