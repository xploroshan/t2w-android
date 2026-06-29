# Live ride tracking — backend + Android scope

**Status:** Scoping / design (no implementation yet)
**Owner:** Mobile
**Depends on:** T2W `/api/v1` backend, `t2w-android` client (this repo)

This is the design for the headline native feature: **background-GPS live ride
tracking**. It's the main reason the app exists — browsers can't do reliable
background location (iOS especially). This doc scopes the backend `/api/v1`
additions and the Android client work so the two can be built against an agreed
contract.

---

## 1. What already exists (don't rebuild)

The backend (`xploroshan/T2W`) already has the full live-tracking **data model
and web logic** — the web app runs live tracking via a Service Worker today. The
relevant Prisma models are already on `main`:

| Model | Role |
|---|---|
| `LiveRideSession` | One per ride: `status` (`waiting`/`live`/`paused`/`ended`), `startedAt`/`endedAt`, `leadRiderId`/`sweepRiderId`, `plannedRoute`, metric overrides, smoothing metadata. |
| `LiveRideLocation` | Raw breadcrumbs: `sessionId`, `userId`, `lat`/`lng`, `speed`, `heading`, `accuracy`, `recordedAt`, `isDeviated`. |
| `LiveRideBreak` | Break intervals: `startedAt`/`endedAt`/`reason`. |
| `LiveRideLocationSmoothed` | Road-snapped / gap-filled tracks (post-processing). |
| `RideMapEdit`, `RideGpxAttachment` | Audit + GPX import/export. |

Server-side helpers already exist for metrics, decimation, deviation and
road-snapping (`src/lib/ride-metrics.ts`, `ride-analytics.ts`, `geo-utils.ts`,
`roads-api.ts`, `route-snap.ts`).

**Implication:** the backend work is mostly **exposing the existing models and
logic over bearer-authed `/api/v1` routes** — not new business logic or schema.

---

## 2. Backend: `/api/v1/rides/{id}/live/*`

New thin route tree under `src/app/api/v1/rides/[id]/live/`, all bearer-authed,
JSON-only, returning the standard error envelope. They wrap the same handlers the
web routes use (per `mobile-apps-plan.md` §5 — extract to `src/lib/api/handlers/`
so web and mobile share logic).

| Method + path | Purpose | Authz |
|---|---|---|
| `GET /api/v1/rides/{id}/live` | Session state: status, started/ended, lead/sweep, participant summary. | Any authed registered rider |
| `POST /api/v1/rides/{id}/live/join` | Join as a tracked rider; returns the session + your role. | Confirmed registrant |
| `POST /api/v1/rides/{id}/live/location` | **Batch** breadcrumb upload (see §4). | Joined rider |
| `POST /api/v1/rides/{id}/live/break` | Start a break. | Joined rider / lead |
| `PATCH /api/v1/rides/{id}/live/break/{breakId}` | End a break. | Joined rider / lead |
| `GET /api/v1/rides/{id}/live/metrics` | Live metrics (distance, avg/max speed, moving time, rider count). | Any authed rider |
| `GET /api/v1/rides/{id}/live/positions` | Latest position per rider (for the live map). | Any authed rider |
| `POST /api/v1/rides/{id}/live/control` | Start / pause / resume / end the session. | `core_member`+ |

### Contract additions to `docs/openapi-v1.yaml`

Schemas to add (the client already had stubs for several of these before the
realignment and can re-adopt them): `LiveSession`, `LiveJoinResponse`,
`LocationPoint`, `LocationBatch`, `LocationBatchAck`, `LiveBreak`, `LiveMetrics`,
`LiveRiderPosition`.

```jsonc
// POST /api/v1/rides/{id}/live/location
{
  "points": [
    { "lat": 12.34, "lng": 77.5, "speed": 41.2, "heading": 270,
      "accuracy": 8.0, "ts": "2026-07-01T06:05:10Z" }
  ],
  "clientBatchId": "uuid"   // idempotency key, see §4
}
// → 202 { "accepted": 47, "duplicates": 3 }
```

### Cross-cutting

- **Decimation server-side** — reuse the existing path-decimation; never trust
  the client to pre-filter. The client uploads everything it recorded.
- **Idempotency** — `clientBatchId` (+ per-point `ts`) so a retried batch after a
  flaky upload doesn't double-insert. Dedup on `(sessionId, userId, recordedAt)`.
- **Rate limiting** — reuse the KV limiter; size for ~50 riders × 50 points / 30 s.
- **No schema change required** for v1. Optional later: a `deviceId`/`lastSeenAt`
  on the session participant for "this device is live".

---

## 3. Android client architecture

```
LiveRideScreen (Compose: map + metrics + break/end controls)
   │  observes
TrackingController (singleton in AppContainer)
   ├─ starts/stops ── LocationTrackingService (foreground, type=location)
   │                     └─ FusedLocationProviderClient → breadcrumbs
   ├─ writes ──────── BreadcrumbDao (Room/SQLite, offline-safe queue)
   └─ flush ───────── LiveRepository → LiveApi → POST .../live/location
                         (batches of ~50; WorkManager retry w/ backoff)
```

### Components

- **Permissions flow** — `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`, then
  `ACCESS_BACKGROUND_LOCATION` (API 29+, requested *separately* after foreground
  grant), `POST_NOTIFICATIONS` (API 33+), and the `FOREGROUND_SERVICE` /
  `FOREGROUND_SERVICE_LOCATION` (API 34+) manifest permissions. A **pre-prompt
  rationale** screen explains "only during an active ride" before the system
  dialog (Play review + iOS-parity expectation). Offer the battery-optimisation
  exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) with an explainer.
- **`LocationTrackingService`** — a foreground service with a persistent
  notification ("T2W is tracking your ride"). Started when the user joins a live
  ride, stopped on end/leave. Holds the `FusedLocationProviderClient` request
  (~5 s interval high accuracy; a **low-power 30 s mode** for long expeditions).
- **Breadcrumb queue** — Room table `breadcrumb(sessionId, lat, lng, speed,
  heading, accuracy, ts, uploaded)`. Survives offline + process death. Flushed in
  batches; rows deleted only after the server acks.
- **Upload** — `LiveRepository.flush()` posts the oldest unsent batch with a
  `clientBatchId`; on failure, **WorkManager** retries with exponential backoff
  when connectivity returns.
- **Networking** — add `LiveApi` (Retrofit) + `LiveRepository`; reuse the
  existing bearer/refresh `OkHttpClient`.
- **Live map** — see §5. Renders own track + last-known positions of other riders
  (poll `…/live/positions` every ~10–15 s while foregrounded), lead/sweep markers.
- **State machine** — `waiting → live → paused(break) → ended`; the service runs
  only while `live && joined`. Persist an "active session" flag so the service
  restarts after process death.
- **End-ride** → post-ride summary + **share card** (render a 1080×1920 Compose
  layout to a `Bitmap` via `GraphicsLayer.toImageBitmap()` / `PixelCopy`, then the
  Android share sheet).

### New dependencies

`play-services-location`, `maps-compose` + `play-services-maps` (if Google),
`androidx.room` (runtime/ktx + KSP compiler — first annotation processor in the
project), `androidx.work:work-runtime-ktx`.

---

## 4. Breadcrumb upload protocol

1. Service delivers a fix → insert into Room (`uploaded = false`).
2. When `count(unuploaded) ≥ 50` **or** every 30 s, take the oldest ≤50, build a
   batch with a fresh `clientBatchId`, `POST …/live/location`.
3. On `202`, mark those rows uploaded (delete). On network failure, keep them and
   schedule a WorkManager retry (2s→4s→…→cap), preserving order.
4. Server decimates + dedups; the client never drops points itself (so a bad GPS
   stretch is reconstructable / GPX-importable as the web already supports).

---

## 5. Map provider decision

| Option | Pros | Cons |
|---|---|---|
| **Google Maps** (`maps-compose`) | Matches web; familiar; good Compose support. | Needs an API key with Android bundle/SHA restriction + cost monitoring. |
| osmdroid / MapLibre | No key, no cost. | Different tiles from web; more glue. |

**Recommendation:** Google Maps to match the web experience, with the key stored
via `secrets.properties` / manifest placeholder (never committed) and restricted
to the app's package + signing SHA. Budget alert as per `mobile-apps-plan.md`.

---

## 6. Phasing

- **P0 — Backend contract** (~1 wk): `…/live/{join,location,break,metrics,
  positions,control}` routes over existing models; spec + Vitest (auth,
  validation, decimation, idempotency).
- **P1 — Foreground tracking** (~1.5 wk): permissions flow, `LocationTrackingService`,
  Room queue, batched upload + WorkManager retry. **No map yet** — verify points
  land server-side.
- **P2 — Live map + metrics + breaks** (~1.5 wk): map screen, live metrics, break
  start/end, other riders' positions.
- **P3 — End-ride + offline hardening** (~1 wk): post-ride summary, share card,
  low-power mode, process-death recovery, OEM battery explainers.
- **P4 — Multi-rider live view polish** (~1 wk): lead/sweep emphasis, deviation
  alerts, reconnection UX.

## 7. Testing

- **Client unit:** queue ordering, batch sizing, backoff, low-power switching,
  metric formatting.
- **Instrumented:** service start/stop on join/end, permission gating, notification.
- **Backend (Vitest):** route auth/authz, validation, decimation, idempotent
  re-upload, rate limit.
- **Load (k6):** 50 riders × 50 points / 30 s sustained.
- **Manual device matrix:** Pixel, Samsung, Xiaomi, OnePlus — background location
  and OEM battery killers vary wildly; emulators can't validate this.

## 8. Risks

| Risk | Mitigation |
|---|---|
| OEM battery killers stop the foreground service | In-app battery-opt explainer; document per-OEM steps; accept some loss + reconstruct via GPX import (already supported). |
| Play review of `FOREGROUND_SERVICE_LOCATION` + background-location | Foreground-service-type declaration; clear "only during active ride" copy; demo video; privacy form. |
| Battery drain | Adaptive interval, low-power 30 s mode, stop service the instant the ride ends. |
| Map key cost/abuse | Package + SHA restriction, budget alert. |
| Privacy (DPDP / Play Data Safety) | Disclose precise + background location; retention policy; data export. |

## 9. Estimate

~6 engineer-weeks across backend (0.5) + Android (1), excluding store review
latency. P0+P1 (points reliably landing server-side) is the highest-value
milestone and gates everything visual after it.
