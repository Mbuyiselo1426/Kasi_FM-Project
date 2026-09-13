# Kasie FM — Android App

Android client for the Kasie FM cloud-native radio project. Talks to the
[Kasie FM API](https://github.com/Mbuyiselo1426/Kasifm-api) over HTTPS/JSON —
no shared code between the two repos, only a shared API contract.

> **Status note:** Kasie FM 97.1 has granted permission to integrate its
> official live stream in this project. The stream URL is resolved from the
> station backend at playback time; the app does not embed a station stream
> URL.

## What it does

- Streams a live radio feed via ExoPlayer, played and owned by a foreground
  `Service` (`RadioService`) — not the Activity — so playback survives the
  app being backgrounded.
- Pulls the current show schedule live from the backend API
  (`GET /api/schedule`) instead of hardcoded strings.
- Shows loading/error states while the network call is in flight.

## Current-show selection and API contract

`HomeFragment` passes the schedule to `ScheduleSelector.findCurrentShow`.
The selector scans for a show whose JSON `isCurrent` field is `true`; it
never assumes item 0 is current and does not calculate current status from
`startTime` / `endTime`. Missing or null flags are treated as false, and null
list entries are ignored. If multiple shows are marked current, the first
marked entry wins, so order independence requires at most one current show.

The Android client expects `GET /api/schedule` to return objects containing
`id`, `name`, `presenter`, `startTime`, `endTime`, `description`, and the
boolean field named exactly `isCurrent`. The backend returns at most
one `isCurrent: true` per response, or all false when no show is current.
Schedule ordering is irrelevant to selection under that contract.

### Implementation and verification

The backend now emits `isCurrent`, evaluated once per response in
`Africa/Johannesburg`. Intervals include their start and exclude their end;
overnight intervals cross midnight, and equal start/end is zero duration.
The current schema repeats all rows daily (including weekends). Overlaps
select the lowest database ID. Responses sort by start time then ID, but
Android selection does not depend on that order.

Successful empty lists and nonempty lists with no current marker both display
`No current show` / `No scheduled show right now`. HTTP failures and missing
response bodies retain their separate unavailable/error states. A schedule
gap makes no claim about whether the audio stream is broadcasting.

JVM tests cover selection at each list position, null/empty inputs and flags,
and Gson JSON mapping with shuffled responses. Backend tests verify marker
production and Jackson serialization with a fixed clock. The backend source
is maintained in `../Kasifm-api`; deploying it remains a separate step.

Selection reflects the response instant. Automatic refresh at show boundaries
is still a follow-up; the visible card can become stale until reloaded.

Run the Android tests with a compatible JDK and Android SDK:

```bash
./gradlew :app:testDebugUnitTest --tests com.kasiefm.ScheduleSelectorTest --tests com.kasiefm.ScheduleJsonTest
```

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

- Featured / Highlights / News bottom-nav tabs currently show static
  placeholder text, not live API data — only the Home tab is wired to the
  backend so far.
- ExoPlayer is on 2.18.7; migration to `androidx.media3` is planned but not
  started.

## Roadmap

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
