---
name: ultra-review
description: Deep, adversarially-verified multi-agent code review of this t2w-android (Kotlin/Compose) repo. Use when the user asks to "ultra-review", "deep review", "thoroughly review/audit", or "do a rigorous review" of the working changes, a branch, or a PR — anything beyond a quick read-through.
argument-hint: "[scope: working diff | branch <ref> | PR #<n> | path glob] (default: whole repo)"
---

# ultra-review — t2w-android

A rigorous review pass that fans out **one finder per dimension**, then **adversarially
verifies every finding against the real source** before reporting it. This is the
project-local equivalent of `/code-review ultra`, encoded as a reusable workflow so the
review is reproducible and the verify step is never skipped.

It is the opposite of a skim: most "findings" from a single read are wrong or
overstated, so nothing survives here until an independent skeptic, reading the actual
Kotlin/Compose code, can state a concrete failure scenario.

## When to use

Invoke when the user wants depth, not a glance: "ultra-review this", "deep/thorough
review", "audit the live-location service", "review PR #42 properly". For a quick sanity
read, just read the diff directly — don't spin up the fleet.

## How to run it

The orchestration lives in [`review-workflow.js`](review-workflow.js). Run it with the
**Workflow** tool — do not re-implement it inline:

```
Workflow({
  scriptPath: ".claude/skills/ultra-review/review-workflow.js",
  args: {
    repoRoot: "<absolute path to this repo checkout>",
    scope:    "<what to focus on>",   // see Scoping below
    depth:    "standard"               // "quick" | "standard" | "deep"
  }
})
```

Always pass `repoRoot` as the **absolute** path of the current checkout so the finder
and verifier subagents read the right files.

### Scoping (`args.scope`)

Free text injected into every agent's brief. Match it to the request:

- **Working changes** → `"the uncommitted working-tree diff (git diff) plus staged changes"`.
  First run `git --no-pager diff --stat` yourself so you can name the touched paths.
- **A branch / PR** → `"branch <ref> vs main"` or `"the changes in PR #<n> vs main"`.
  Resolve the PR's head branch/files first, then pass the path list.
- **A subsystem** → a glob, e.g. `"app/src/main/kotlin/com/taleson2wheels/app/data/location/*"`.
- **Whole repo** (default) → omit `scope`.

### Depth (`args.depth`) — scale to the ask

| depth      | finders | verify           | use when                                            |
|------------|---------|------------------|-----------------------------------------------------|
| `quick`    | 5 dims  | 1 verifier/finding | "find bugs", small diff, fast pass                |
| `standard` | 5 dims  | 1 verifier/finding | default                                             |
| `deep`     | 5 dims  | 3 verifiers/finding, distinct lenses, majority rules | "thoroughly audit", "be exhaustive", security-sensitive |

The five dimensions are networking/auth, repositories/offline-cache, ViewModels/Compose
UI, the background-location foreground service, and manifest/build/security config.

## Verify ceiling (what finders can actually run here)

There is **no emulator or device** in this sandbox, so instrumented (`androidTest`) and
Paparazzi screenshot tests don't run. The real, runnable gates — finders are told to
reason from source for anything beyond them — are:

```bash
./gradlew :app:testDebugUnitTest   # JVM unit tests
./gradlew :app:assembleDebug       # compile + build the debug APK
```

## Reporting the result

The workflow returns `{ counts, dimensionSummaries, confirmed, uncertain }`. Report to
the user:

1. A one-line health read per dimension (from `dimensionSummaries`).
2. **CONFIRMED findings only**, ranked most-severe first, each as
   `severity · file:line — what's wrong → concrete fix`. These survived adversarial
   verification.
3. `uncertain` findings in a short separate list (couldn't be confirmed or refuted from
   the code) so the user can decide. Drop `refuted` ones silently — just report the count.

Do not pad the report with the refuted noise; the point of the verify pass is that you
only stand behind what survived it.
