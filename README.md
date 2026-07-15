# Kasie FM — Android App

Android client for the Kasie FM cloud-native radio project. Talks to the
[Kasie FM API](https://github.com/Mbuyiselo1426/Kasifm-api) over HTTPS/JSON —
no shared code between the two repos, only a shared API contract.

> **Status note:** this is a student prototype inspired by the real Kasie FM
> 97.1 community station (Katlehong / Thokoza, Ekurhuleni). The real station
> has **not yet given permission** for its branding, live stream, or full
> schedule to be used. The logo currently in this repo is theirs and used
> only for a private demo; nothing here is public or affiliated with the
> station until they say otherwise. See the project journey log for details.

## What it does

- Streams a live radio feed via ExoPlayer, played and owned by a foreground
  `Service` (`RadioService`) — not the Activity — so playback survives the
  app being backgrounded.
- Pulls the current show schedule live from the backend API
  (`GET /api/schedule`) instead of hardcoded strings.
- Shows loading/error states while the network call is in flight.

## Architecture — why a Service owns the player

Early versions had both `MainActivity` and `RadioService` independently
creating their own `ExoPlayer`, which meant the foreground service existed in
code but wasn't actually reachable from the UI, and playback died the moment
the Activity did. The current design:

- `RadioService` is the **only** owner of the `ExoPlayer` instance, exposes
  `play()` / `pause()` / `isPlaying()` / `getCurrentState()` through a
  `Binder`, and reports state changes back via a `PlaybackStateListener`
  callback.
- `MainActivity` **starts** the service (`startForegroundService`, so it
  survives independently of the Activity) and **binds** to it
  (`bindService`, so it can call methods and receive callbacks) in
  `onStart()`, then unbinds — without stopping the service — in `onStop()`.
- On rebind (e.g. returning from Home), the Activity immediately calls
  `getCurrentState()` to sync the UI, rather than waiting passively for the
  next state-change callback, which may never come if nothing changes while
  the app was backgrounded.

## Setup

Requires the [Kasie FM API](https://github.com/Mbuyiselo1426/Kasifm-api)
running somewhere reachable — either locally via Docker or the live Render
deployment. Point `RetrofitClient`'s base URL at whichever you're using.

Build and run like any Android project (Android Studio, or via the GitHub
Actions CI pipeline below if your machine can't comfortably run the
emulator).

### Building without a local emulator

If your machine is resource-constrained, this repo's GitHub Actions workflow
compiles a debug APK on every push. Download the artifact from the Actions
tab and side-load it onto a physical device (`Install unknown apps` from
whichever app you download it through) — no local Android Studio build
required.

## Known issues

- `/api/stream-url` is not yet wired into `RadioService` — it still points
  at a SomaFM placeholder stream. This is intentional until the real Kasie
  FM stream URL is confirmed with the station.
- Featured / Highlights / News bottom-nav tabs currently show static
  placeholder text, not live API data — only the Home tab is wired to the
  backend so far.
- ExoPlayer is on 2.18.7; migration to `androidx.media3` is planned but not
  started.

## Roadmap

- Wire `/api/stream-url` into `RadioService` (pending Kasie FM's real stream
  URL and permission).
- Migrate to `androidx.media3`.
- Data Lite mode for low-data usage.
- Push notifications for show reminders.
- Point the app at the eventual AWS-hosted backend instead of Render.

## a new update i must change at navbar
Better navigation

Home

Featured

Highlights

Requests

News
