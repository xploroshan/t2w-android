# Project skills (`.claude/skills/`)

Skills committed here are available to every contributor's Claude Code session in this
repo. Newly added project skills are discovered at **session start**, so after pulling
this directory you may need to reload/restart your Claude Code session before they appear
in the `/` menu.

## `ultra-review/` — authored in this repo

A deep, adversarially-verified multi-agent code review, encoded as a reusable Workflow
script (`review-workflow.js`) plus an invocation guide (`SKILL.md`). It is the
project-local equivalent of the built-in `/code-review ultra`: it fans out one finder per
review dimension, then makes an independent skeptic re-read the real source and try to
*refute* each finding before it is reported. Only findings that survive verification are
surfaced.

Tuned for **this** repo (t2w-android, native Kotlin + Jetpack Compose): the dimensions
cover networking/auth, repositories/offline-cache, ViewModels/Compose UI, the
background-location foreground service, and manifest/build/security config. The finders
are told the local verify ceiling — `./gradlew :app:testDebugUnitTest` and
`./gradlew :app:assembleDebug` (no emulator/device, so instrumented + Paparazzi tests
don't run here).

Authored locally; not third-party. Edit it freely as the codebase evolves.

## `ui-ux-pro-max/` + `banner-design/`, `brand/`, `design/`, `design-system/`,<br>`slides/`, `ui-styling/` — vendored third-party

The full **UI/UX Pro Max** skill suite, vendored verbatim from the official distribution.

- **Source:** `github.com/nextlevelbuilder/ui-ux-pro-max-skill`
- **Installer:** `ui-ux-pro-max-cli` (`uipro`) v2.10.0 — `npx ui-ux-pro-max-cli init --ai claude`
- **Contents:** 7 skills, ~145 files (markdown guidance + CSV/JSON design data + a few
  Python/`.cjs` helper scripts). No binaries; no secrets. Upstream license preserved at
  `ui-styling/LICENSE.txt`.

These are **third-party instructions and data that Claude will read and follow** when
invoked. They were reviewed before committing and contain design guidance only. If you
re-vendor or update the suite, do it from the official source above and re-review the
diff. The suite is web-leaning, but `ui-ux-pro-max/data/stacks/jetpack-compose.csv`
carries Compose-specific guidance relevant to this app.

> Footprint note: the suite adds ~3 MB of mostly-CSV design data to the repo. It's
> data-only and inert unless a skill is invoked. If the size is unwelcome, the suite can
> be slimmed to just `ui-ux-pro-max/` (the core skill is self-contained) or removed
> without affecting `ultra-review`.
