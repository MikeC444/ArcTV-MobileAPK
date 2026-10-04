# Working notes for Claude Code

- This is the Android **phone and tablet** build of Arc TV. It started as a copy of the Fire TV app (`MikeC444/ArcTV-AndroidTV`), the backend lives
  there too (`server/`), and the web app is `MikeC444/ArcTV-Web`. A fix to shared code (player, sync, sources, addons) usually belongs in both apps:
  say so to the user, or make it in both.
- Add each user-visible change as a bullet under `## Unreleased` in `RELEASE_NOTES.md` (plain, short, upbeat, no file names: it is what the update
  pop-up shows). Log engineering work in `CHANGELOG.md`. Cutting a release is a separate, explicit request.
- There is normally no Android SDK in the cloud sandbox, so builds are checked by pushing to a `claude/**` branch and reading the **Build debug APK**
  workflow run. Say plainly what was and was not built or run.
