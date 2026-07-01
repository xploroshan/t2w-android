# Maestro E2E flows

End-to-end UI flows for the **Tales on 2 Wheels** Android app
(`com.taleson2wheels.app`), written for [Maestro](https://maestro.mobile.dev).

Selectors are **text-based** and target the real on-screen copy
(`res/values/strings.xml` + composable literals), so they survive layout and
restyle changes. They intentionally avoid brittle view-ID / index selectors.

## Flows

| File | What it covers |
| --- | --- |
| `01_launch_to_login.yaml` | Cold launch on a signed-out install lands on the login screen; asserts title, subtitle, field labels, button, and the forgot/register links. |
| `02_login_form_validation.yaml` | The login form keeps "Sign in" inert on blank input, accepts a well-formed email + password, and dispatches the request without crashing. |
| `03_tab_navigation.yaml` | Signs in, then walks the bottom tabs Home → Rides → Riders → Profile, asserting each destination's app-bar text. |
| `04_register_navigation.yaml` | From login, opens the "Create account" flow (email → verification-code step) and returns. No backend. |
| `05_forgot_password_navigation.yaml` | From login, opens the "Forgot password" reset flow (email → reset-code step) and returns via "Back to sign in". No backend. |
| `06_admin_and_relive_journeys.yaml` | Signs in as an admin, opens the Admin console (asserts all five hub entries + Content moderation), then opens a completed ride's "Relive in 3D". **Needs a seeded admin backend.** |

## Prerequisites

- Maestro CLI installed: `curl -Ls "https://get.maestro.mobile.dev" | bash`
- A connected Android emulator or device with the **debug** APK installed
  (see [`../docs/SIDELOAD.md`](../docs/SIDELOAD.md) to build and `adb install`).
- For `03_tab_navigation.yaml`: a backend reachable at the app's
  `API_BASE_URL` (debug default `http://10.0.2.2:3000/`, i.e. the host's
  `localhost:3000` from the emulator) with a **seeded login account**.

## Running

Run the whole suite (Maestro picks up every `*.yaml` in the folder):

```bash
maestro test .maestro
```

Run a single flow:

```bash
maestro test .maestro/01_launch_to_login.yaml
```

Pass seeded credentials to the tab-navigation flow without committing secrets:

```bash
maestro test \
  -e EMAIL=you@example.com \
  -e PASSWORD=your-password \
  .maestro/03_tab_navigation.yaml
```

Interactive selector exploration (useful when adding flows):

```bash
maestro studio
```

## Notes

- `clearState` at the top of each flow wipes the DataStore session so runs start
  signed-out and deterministic.
- **No backend needed** (these run in the CI emulator gate): `01`, `02`, `04`,
  `05` — they exercise the auth gate and client-side navigation/form behaviour
  only. Flow 02's post-submit assertion is deliberately lenient (the login title
  remaining proves the form round-tripped); point it at a seeded backend and swap
  in a home-screen assertion for a true happy-path login.
- **Backend required** (run manually with `-e EMAIL=… -e PASSWORD=…`): `03` needs
  any seeded account; `06` needs a **core_member/superadmin** account (admin
  console) plus a completed ride with a recorded track (Relive) — adjust its
  ride-title selector to your seeded data.
- CI: the emulator workflow runs the no-backend flows (`01 02 04 05`). Maestro
  Cloud or a self-hosted runner with a seeded backend can execute the full
  `maestro test .maestro`. Keep credentials in CI secrets and inject them with `-e`.
