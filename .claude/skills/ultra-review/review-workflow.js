// ultra-review workflow — t2w-android (native Kotlin + Jetpack Compose app).
//
// Multi-agent deep code review: fan out one finder per review DIMENSION, then
// adversarially VERIFY every finding against the real source before it is
// allowed to survive. Pipelined — a dimension's findings start verifying the
// moment that dimension finishes, while other dimensions are still reviewing.
//
// Invoked by the `ultra-review` skill via the Workflow tool:
//   Workflow({ scriptPath: ".claude/skills/ultra-review/review-workflow.js",
//              args: { repoRoot: "<abs repo path>", scope: "...", depth: "deep" } })
//
// args (all optional):
//   repoRoot : absolute path to the repo checkout (default ".")
//   scope    : free-text focus, e.g. "the working-tree diff", "PR #123",
//              "branch feat/x vs main", "app/src/main/.../data/location/*". Default: whole repo.
//   depth    : "quick" | "standard" | "deep"  (default "standard")
//              quick   -> medium finder effort, single-vote verify
//              standard-> high finder effort,   single-vote verify
//              deep    -> high finder effort,   3-vote adversarial verify (majority rules)
export const meta = {
  name: 'ultra-review',
  description: 'Deep multi-agent review of the t2w-android app: find defects by dimension, adversarially verify each against the real Kotlin/Compose code',
  phases: [
    { title: 'Review' },
    { title: 'Verify' },
  ],
}

const A = (typeof args === 'object' && args) ? args : {}
const REPO = A.repoRoot || '.'
const SCOPE = A.scope || 'the entire repository'
const DEPTH = ['quick', 'standard', 'deep'].includes(A.depth) ? A.depth : 'standard'
const FINDER_EFFORT = DEPTH === 'quick' ? 'medium' : 'high'
const VOTES = DEPTH === 'deep' ? 3 : 1

const FINDINGS_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  properties: {
    dimension: { type: 'string' },
    summary: { type: 'string', description: 'one-line health read of this area' },
    findings: {
      type: 'array',
      items: {
        type: 'object',
        additionalProperties: false,
        properties: {
          title: { type: 'string' },
          severity: { type: 'string', enum: ['critical', 'high', 'medium', 'low', 'info'] },
          category: { type: 'string', enum: ['security', 'correctness', 'bug', 'contract', 'reliability', 'performance', 'ux', 'maintainability'] },
          file: { type: 'string', description: 'repo-relative path' },
          line: { type: 'integer' },
          description: { type: 'string', description: 'what is wrong, concretely' },
          impact: { type: 'string', description: 'what breaks for the user/system' },
          suggestion: { type: 'string', description: 'how to fix' },
        },
        required: ['title', 'severity', 'category', 'file', 'description', 'impact', 'suggestion'],
      },
    },
  },
  required: ['dimension', 'summary', 'findings'],
}

const VERDICT_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  properties: {
    verdict: { type: 'string', enum: ['confirmed', 'refuted', 'uncertain'] },
    confidence: { type: 'string', enum: ['high', 'medium', 'low'] },
    correctedSeverity: { type: 'string', enum: ['critical', 'high', 'medium', 'low', 'info'] },
    reasoning: { type: 'string' },
    failureScenario: { type: 'string', description: 'concrete inputs/state -> wrong outcome, or empty if refuted' },
  },
  required: ['verdict', 'confidence', 'reasoning'],
}

// Verify ceiling the finders can rely on (no emulator/device in CI sandbox):
//   ./gradlew :app:testDebugUnitTest    (JVM unit tests)
//   ./gradlew :app:assembleDebug        (compiles + builds the APK)
// Instrumented (androidTest) + Paparazzi run only on a device/Linux runner.
const common = `You are doing a rigorous code review of a native Kotlin / Jetpack Compose Android app. The repository is checked out at ${REPO}. FOCUS: ${SCOPE}. READ the actual source files with Read/Grep/Glob — do NOT guess or rely on memory. Report only REAL defects that affect correctness, security, reliability, the app<->API contract, or user-facing functioning. Skip pure style/naming nits. Every finding must be anchored to a real repo-relative file path and line, with a concrete description, impact, and fix. Prefer fewer, higher-quality findings (up to ~10, most severe first) over speculation. If the area is healthy, return few or zero findings and say so in the summary. The local verify ceiling is \`./gradlew :app:testDebugUnitTest\` (JVM tests) and \`./gradlew :app:assembleDebug\` (compile + APK) — there is no emulator/device, so instrumented tests don't run; reason from source.`

const DIMENSIONS = [
  { key: 'app-net', prompt: `${common}
AREA: Networking & auth (client). Review app/src/main/kotlin/com/taleson2wheels/app/data/remote/* (Retrofit api/*, AuthInterceptor, TokenAuthenticator, ApiError, ApiResult, safeApiCall, the Retrofit/OkHttp setup), data/session/* (SessionStore, token cipher/Keystore), and di/AppContainer.kt.
LOOK FOR: 401-refresh recursion or thundering-herd (concurrent refresh), token persistence/at-rest flaws, refresh token not rotated/used, error mapping gaps (HttpException vs IOException), wrong base-url/path joining (double slashes, missing api/v1 prefix), missing timeouts, interceptor adding auth to the refresh call, serialization config issues. Return findings with dimension="app-net".` },

  { key: 'app-data', prompt: `${common}
AREA: Repositories & offline cache (client). Review app/src/main/kotlin/com/taleson2wheels/app/data/repository/* and data/local/* (Room: CachedResponse, AppDatabase, CacheStore/RoomCacheStore, ResponseCache + networkWithFallback) and data/remote/dto/* parsing.
LOOK FOR: stale-fallback semantics bugs (masking auth/HTTP errors, serving stale wrongly), cache-key collisions or per-user leakage across logins, Room threading/main-thread access, schema/migration hazards, DTO @Serializable mismatches with the API (nullable vs non-null, defaults), pagination/cursor handling in repos, swallowed errors. Return findings with dimension="app-data".` },

  { key: 'app-ui', prompt: `${common}
AREA: ViewModels & Compose UI (client). Review app/src/main/kotlin/com/taleson2wheels/app/ui/* (ViewModels and screens across home, rides, live, riders, blogs, profile, garage, achievements, notifications, auth, content, common) and MainScreen.kt / AppViewModelFactory.kt.
LOOK FOR: state races / lost updates in mutableStateOf flows, LaunchedEffect with wrong/missing keys (re-runs or stale captures), unguarded pagination triggers, navigation arg decoding bugs (route param collisions, unescaped args), missing error/empty/loading states, recomposition performance traps, coroutine scope/lifecycle leaks, factory wiring mismatches. Return findings with dimension="app-ui".` },

  { key: 'app-service', prompt: `${common}
AREA: Background-location foreground service (client, newest & riskiest). Review app/src/main/kotlin/com/taleson2wheels/app/data/location/{LiveLocationService,LiveShareController,LocationTracker}.kt and ui/live/{LiveRideScreen,LiveRideViewModel}.kt plus the <service> + permissions in AndroidManifest.xml.
LOOK FOR: foreground-service lifecycle bugs (startForeground timing -> ANR/crash on Android 12+/14), runtime permission gating (ACCESS_FINE/BACKGROUND_LOCATION, POST_NOTIFICATIONS) before start, START_REDELIVER_INTENT ride-id recovery, flush/retry/re-queue correctness on failure, process-wide StateFlow leaks or staleness across rides, battery/wakelock, missing stopForeground/stopSelf paths, notification channel creation. Return findings with dimension="app-service".` },

  { key: 'app-config', prompt: `${common}
AREA: Manifest, build & security config (client). Review app/src/main/AndroidManifest.xml, app/build.gradle.kts, app/proguard-rules.pro, and res/xml/{network_security_config,backup_rules,data_extraction_rules}.xml if present.
LOOK FOR: components exported unintentionally, over-broad permissions, missing R8/ProGuard keep rules for Retrofit/kotlinx-serialization/Room (release-only crashes from stripped serializers/models), release-signing fallback risks, cleartext traffic allowed too broadly, secrets baked into the APK, foregroundServiceType correctness, missing allowBackup considerations for tokens. Return findings with dimension="app-config".` },
]

const verifyPromptFor = (f, lens) => `You are an adversarial verifier${lens ? ` reviewing through a ${lens} lens` : ''}. A code reviewer claims a defect in the Kotlin/Compose app at ${REPO}. Your job is to REFUTE it unless the code genuinely exhibits it. READ the actual file (${f.file}${f.line ? `, around line ${f.line}` : ''}) and any related code before deciding — do not trust the claim.

CLAIM: ${f.title}
SEVERITY (claimed): ${f.severity}
DESCRIPTION: ${f.description}
IMPACT: ${f.impact}

Decide: "confirmed" only if you can state a concrete failure scenario (specific inputs/state -> wrong output/crash/security hole) grounded in the real code; "refuted" if the code does not actually have this problem (explain why the reviewer was wrong); "uncertain" if you genuinely cannot tell from the code. Adjust severity if the reviewer over/under-stated it. Default toward refuted/uncertain when unsure.`

// deep mode gives each of the 3 verifiers a distinct lens so they catch
// different failure modes rather than echoing one another.
const LENSES = ['correctness', 'security', 'does-it-actually-reproduce']
const verifyOne = (f) => {
  if (VOTES === 1) {
    return agent(verifyPromptFor(f), { label: `verify:${f.dimension}:${(f.file || '?').split('/').pop()}`, phase: 'Verify', schema: VERDICT_SCHEMA })
      .then((v) => ({ ...f, verdict: v }))
      .catch(() => ({ ...f, verdict: { verdict: 'uncertain', confidence: 'low', reasoning: 'verification agent failed' } }))
  }
  return parallel(LENSES.slice(0, VOTES).map((lens) => () =>
    agent(verifyPromptFor(f, lens), { label: `verify:${f.dimension}:${lens}:${(f.file || '?').split('/').pop()}`, phase: 'Verify', schema: VERDICT_SCHEMA }).catch(() => null),
  )).then((votes) => {
    const v = votes.filter(Boolean)
    const c = v.filter((x) => x.verdict === 'confirmed').length
    const r = v.filter((x) => x.verdict === 'refuted').length
    const verdict = c > r ? 'confirmed' : (r >= c && r > 0 ? 'refuted' : 'uncertain')
    const worst = v.map((x) => x.correctedSeverity).filter(Boolean).sort((a, b) => sevRank[a] - sevRank[b])[0]
    return { ...f, verdict: { verdict, confidence: c >= 2 || r >= 2 ? 'high' : 'low', correctedSeverity: worst, reasoning: v.map((x) => x.reasoning).join(' | '), votes: { confirmed: c, refuted: r, total: v.length } } }
  })
}

const sevRank = { critical: 0, high: 1, medium: 2, low: 3, info: 4 }

const reviewed = await pipeline(
  DIMENSIONS,
  (d) => agent(d.prompt, { label: `review:${d.key}`, phase: 'Review', schema: FINDINGS_SCHEMA, effort: FINDER_EFFORT }),
  (review, d) => {
    const findings = (review && review.findings) ? review.findings.map((f) => ({ ...f, dimension: d.key })) : []
    if (!findings.length) return { dimension: d.key, summary: review && review.summary, findings: [] }
    return parallel(findings.map((f) => () => verifyOne(f)))
      .then((verified) => ({ dimension: d.key, summary: review && review.summary, findings: verified }))
  },
)

// A pipeline stage that throws (e.g. a finder hit a transient "Overloaded"
// API error) drops that item to null and skips its verify stage. Filter those
// out before aggregating, and surface which dimensions were lost so reduced
// coverage is never silent.
const ok = reviewed.filter(Boolean)
const droppedDims = DIMENSIONS.filter((_, i) => !reviewed[i]).map((d) => d.key)
if (droppedDims.length) log(`⚠️ ${droppedDims.length} dimension(s) dropped (finder failed, not reviewed): ${droppedDims.join(', ')}`)

const summaries = ok.map((r) => ({ dimension: r.dimension, summary: r.summary }))
const all = ok.flatMap((r) => (r.findings || []))
const effSev = (f) => (f.verdict && f.verdict.correctedSeverity) || f.severity
const bySev = (a, b) => sevRank[effSev(a)] - sevRank[effSev(b)]

const confirmed = all.filter((f) => f.verdict && f.verdict.verdict === 'confirmed').sort(bySev)
const uncertain = all.filter((f) => f.verdict && f.verdict.verdict === 'uncertain').sort(bySev)
const refutedCount = all.filter((f) => f.verdict && f.verdict.verdict === 'refuted').length

log(`ultra-review (${DEPTH}, ${VOTES}-vote): ${all.length} raw -> ${confirmed.length} confirmed, ${uncertain.length} uncertain, ${refutedCount} refuted`)

return {
  repo: 't2w-android',
  scope: SCOPE,
  depth: DEPTH,
  counts: { raw: all.length, confirmed: confirmed.length, uncertain: uncertain.length, refuted: refutedCount },
  droppedDimensions: droppedDims,
  dimensionSummaries: summaries,
  confirmed,
  uncertain,
}
