# DX review — `spring-aiact`

> Scope: developer experience of adopting `spring-aiact` as a new user, judged against a
> "Laravel-like" / batteries-included bar (works out of the box, great getting-started, intuitive
> API, helpful errors). Review only, no code changes. Cited to files at the state of `main` on
> 2026-06-08 (after Art.50 transparency, JDBC sink, risk-class + Art.5 gate, Art.15 accuracy,
> FRIA design-only landed).

## Verdict in one paragraph

`spring-aiact` is **already a DX-first library on the dimensions that are hardest to retrofit**:
zero-config autoconfiguration with sane defaults, fail-fast refusals that name the exact fix,
deny-by-default security, and a README that is genuinely excellent. Where it falls short of the
bar is **uneven batteries-included coverage**: the three newest capabilities (JDBC sink, Art.15
accuracy enforcement, Art.50 transparency filter) are prominent in the README but are *not* wired
into the autoconfiguration the way the audit chain is, so the "annotate and it works" promise
silently degrades for them. Secondary gaps are install friction (JitPack ceremony, no BOM) and an
undocumented sample app. None are deep; all are shippable in a focused pass.

---

## DX scorecard

| Dimension | Grade | Strength | Gap |
|---|---|---|---|
| **1. Install** | B | True one-liner exists; dual-line table maps Spring Boot version → tag pin (`README.md` L197-216). | JitPack ceremony: `<repositories>` + `<pluginRepositories>` + `com.github.iambilotta.spring-aiact` group + `v`-prefixed tag is 4 things to get right vs a 3-coord Maven Central GAV. No BOM, so the starter and plugin versions are pinned independently and can drift. |
| **2. Zero-config default** | A | `@ConditionalOnProperty(matchIfMissing=true)` + every bean `@ConditionalOnMissingBean` (`AiActAutoConfiguration.java` L34-35, 60-171): drop the starter in, get the full audit pipeline with no config. Defaults are real (NDJSON `aiact-logs/`, `P10Y` retention, `single-writer-lock=true`, deny-all guard). Fail-fast on the `change-me-please` HMAC secret in prod (L65-80). Startup reporter prints one orientation line + warnings (`AiActStartupReporter.java` L53-70). | The default `UserPseudonymizer.noop()` (L104-106) silently passes usernames through; safe-by-default would lean the other way, or at least warn at boot the way the HMAC/guard paths do. |
| **3. API ergonomics** | B+ | Annotations read like the regulation they encode; each carries article-level javadoc (`AiActLog`, `AiActHighRiskSystem`, `AiActTransparency`, `AiActRiskClass`). 140 lines of IDE config metadata give autocomplete + hover docs for every `aiact.*` key (`additional-spring-configuration-metadata.json`). `RiskClassifier` has a clean, documented resolution order + a `Hook` SPI for richer policies (`RiskClassifier.java` L12-50). `AiActEndpointGuard` is a single-method SPI returning a typed `Decision` (`AiActEndpointGuard.java`). | Five companion annotations on one class (`HiringScreener.java` L24-62) is a lot of surface to hand-author with no IDE template/live-template; an annotation that *aggregates* the common case (or an archetype/snippet) would cut the cold-start. `AccuracyEnforcer` is a static façade adopters must call themselves from a test — there is no autowired bean or `@Tag` hook, so Art.15 "enforcement" is a manual integration, not a default. |
| **4. Getting-started** | B | README "Quick start" is a real 5-step copy-paste path (L193-268) and "The pitch in 30 seconds" doubles as a runnable mental model with a tamper demo (L37-102). A Docker Compose example exists (`examples/docker-compose/`). | "~15 min first run" / "~1h production-ready" (L18-19) is asserted, not demonstrated by a single runnable scaffold. The **sample app has no README** (`spring-aiact-sample/` ships code but no `README.md`): the one artifact that should be a 5-minute "clone, run, curl" path makes the reader reverse-engineer `application.yaml` and the test classes. The quickstart also interleaves starter + plugin + two repository blocks, so the minimal "just see it log" path is longer than it needs to be. |
| **5. Errors** | A | Best-in-class fail-fast. The HMAC refusal names the offending value, the property to set, the dev-profile escape, *and* the (discouraged) override flag in one message (`AiActAutoConfiguration.java` L68-74). The Maven plugin reports each missing companion **by class + by Article** ("missing @AiActOversight (Article 14)", `HighRiskAnnotationValidator.java` L34-44) and refuses Art.5 prohibited practices unconditionally (`VerifyMojo.java` L56-66). `AccuracyEnforcer` parse errors quote the bad threshold string (`AccuracyEnforcer.java` L94-101). `AuditLogService.writeJsonLine` default throws a *named* `UnsupportedOperationException` instead of silently emitting a wrong shape (`AuditLogService.java` L53-56). | The `RetentionPolicyService requires NdjsonAuditLogService` failure (L144-146) only surfaces at bean-creation time for adopters who swap in the JDBC sink — a known sharp edge tied directly to gap #1 below. |
| **6. Docs** | A- | README is comprehensive and honest (an explicit "Reality check" of what the library is NOT, L380-390). 8 ADRs with rationale + rejected alternatives. `PRODUCTION.md` is an 8-section operational runbook (HMAC outside the tree, guard wiring, multi-pod caveats, key rotation, retention export, CI gate). JMH-measured perf numbers with a re-run command (L336-355). Javadoc is dense and useful. | No published javadoc/site (adopters read source for the SPI contracts of `AuditLogService` and the JDBC sink's `initSchema`). The JDBC sink's adoption path lives **only** in code comments (`JdbcAuditLogService.java` L58-75), not in README or PRODUCTION.md, despite being a headline new feature. |
| **7. Batteries vs explicit-binding balance** | B- | The deliberate explicit bindings are the *right* ones: deny-by-default guard, HMAC secret, single-writer-lock are all decisions a compliance tool MUST force. The auth SPI keeping `core` Spring-Security-free is correct (`AiActEndpointGuard.java` L7-18). | The balance tips the wrong way for the new features. `JdbcAuditLogService` is **not** referenced anywhere in the starter (no autoconfig, no `@ConditionalOnProperty(aiact.audit.sink=jdbc)`); adopting it is hand-wiring a bean + calling `initSchema()` + replacing the default `RetentionPolicyService`. `AccuracyEnforcer` and the `AiActTransparencyFilter` mapping are similarly under-batteried relative to their README prominence. A new adopter who reads "Article 15 enforced" / "JDBC sink" and then finds no `aiact.*` switch for either will feel the promise/reality gap. |

**Overall: B+.** A-grade on the expensive-to-fix fundamentals (zero-config, errors, docs honesty);
held back from A by batteries-included unevenness on the three newest features and by sample/install
friction.

---

## Is it DX-first / "Laravel-like"?

**Mostly yes, with one structural caveat.** The Laravel test is "the happy path works with no config
and the framework makes the safe choice for you." `spring-aiact` passes that for its *core* (audit
chain + technical file + DoC): drop the starter, annotate one class, `mvn verify` fails loudly until
the dossier is complete, the app boots and logs a tamper-evident chain. That is the batteries-included
experience, and the fail-fast guardrails are *more* opinionated than Laravel (it refuses to boot on a
placeholder secret), which is the correct posture for a compliance tool.

The caveat is **feature-tier inconsistency**: a Laravel-grade library would expose every advertised
capability behind the same one-flag ergonomics. Here the audit chain is fully auto-wired, but JDBC,
accuracy, and transparency are partially manual. The library *feels* batteries-included on first
contact and then asks for hand-wiring exactly where the newest, most-marketed features live. Closing
that gap is what moves it from "DX-first core" to "DX-first library."

---

## Ranked improvements (most impactful first)

Each: **what / why / where**.

### 1. Auto-wire the JDBC sink behind one property (`aiact.audit.sink=ndjson|jdbc`)
- **What:** add `@ConditionalOnProperty(aiact.audit.sink=jdbc)` autoconfig that builds
  `JdbcAuditLogService` from the adopter's `DataSource`, runs `initSchema()` (guarded by a
  `aiact.audit.jdbc.auto-ddl` flag for Flyway/Liquibase owners), and supplies a matching
  `RetentionPolicyService` so the `requires NdjsonAuditLogService` failure can't fire.
- **Why:** the JDBC sink is a README headline and the *correct* default for scale-to-zero / ephemeral
  runtimes, yet today it is invisible to the starter and adopting it trips a bean-creation error.
  Highest promise/reality gap in the library.
- **Where:** new bean in `AiActAutoConfiguration.java` (mirror L110-147); `JdbcAuditLogService.initSchema()`
  already exists (`JdbcAuditLogService.java` L63-75); add the metadata key to
  `additional-spring-configuration-metadata.json`.

### 2. Give the sample app a README + make it the canonical 5-minute path
- **What:** add `spring-aiact-sample/README.md` with literal `mvn spring-boot:run` + the exact `curl`
  commands (verify, head, tamper-and-re-verify, override POST), and the path to the generated
  `target/generated-docs/`. Link it from README step 5 as *the* getting-started.
- **Why:** the "~15 min" claim is currently unbacked by a runnable; the sample is the asset that
  proves it but it's undocumented, so the reader reverse-engineers `application.yaml`. A README turns
  the sample into the copy-paste quickstart the main README promises.
- **Where:** `spring-aiact-sample/` (config already at `src/main/resources/application.yaml`,
  endpoints already permissive for local run).

### 3. Make Art.15 accuracy enforcement a first-class, autowired hook
- **What:** expose an injectable `AccuracyEnforcer` bean (or a JUnit extension / `@Tag("aiact-accuracy")`)
  that reads `@AiActAccuracyMetric` off the class and gates on a supplied measured value, so adopters
  *wire a value*, not *call a static method and remember to fail the build*.
- **Why:** today "Article 15 enforced" reduces to a static utility the adopter must invoke from their
  own test with no scaffolding — declarative annotation, manual gate. The other articles are enforced
  by the build (plugin) or runtime (advisor); accuracy should match that tier.
- **Where:** `AccuracyEnforcer.java` (static façade today, L20-74); candidate bean in the starter or a
  test-scoped artifact; surface the metric model the plugin already parses.

### 4. Publish a BOM and a published javadoc/site
- **What:** ship a `spring-aiact-bom` so adopters import one managed version and the starter/plugin
  can't drift; publish javadoc (JitPack serves `/javadoc`, or GitHub Pages) so the SPI contracts
  (`AuditLogService`, `AiActEndpointGuard`, `JdbcAuditLogService.initSchema`) are readable without
  cloning.
- **Why:** the SPIs are the extension surface; making adopters read source to implement them is the
  one place the otherwise-excellent docs force a context switch. The BOM removes a whole class of
  version-mismatch support questions.
- **Where:** new `bom/pom.xml`; JitPack already builds the reactor (`jitpack.yml`); README install
  section (L204-257).

### 5. Document the JDBC adoption + retention path outside source comments
- **What:** a `PRODUCTION.md` section (or a `docs/PERSISTENCE.md`) covering NDJSON-vs-JDBC trade-offs,
  the DDL, Flyway-owned-schema path, and the retention implication. Cross-link the throughput table.
- **Why:** the only place this lives today is `JdbcAuditLogService.java` L58-75; the sink is a
  deployment decision, which is exactly what `PRODUCTION.md` is for.
- **Where:** `docs/PRODUCTION.md` (already 8 sections, add §9).

### 6. Warn (or fail) on the no-op pseudonymizer the way HMAC/guard do
- **What:** when `UserPseudonymizer.noop()` is the active bean *and* no dev profile is on, emit a
  boot WARN (consistent with the HMAC and allow-all-guard warnings already printed).
- **Why:** raw usernames flowing into audit metadata is a quiet GDPR foot-gun; the library already
  has the boot-warning pattern, this is one more line in the same place.
- **Where:** `AiActStartupReporter.report()` (`AiActStartupReporter.java` L41-71); default set at
  `AiActAutoConfiguration.java` L104-106.

### 7. Reduce annotation cold-start friction (archetype or live-template snippet)
- **What:** ship a Maven archetype, or a documented IntelliJ/VS Code live template, that stamps the
  full high-risk annotation block (the 5 companions) pre-filled with `TODO` markers.
- **Why:** authoring five interlocking annotations from scratch is the steepest part of first contact;
  the plugin already tells you what's missing *after* the fact, a template removes the friction
  *before* it.
- **Where:** new `spring-aiact-archetype` module or a `docs/` snippet; mirror `HiringScreener.java`
  L24-62.

---

## Top 3 DX wins to ship next

1. **Auto-wire the JDBC sink behind `aiact.audit.sink=jdbc`** (improvement #1). Single highest-leverage
   fix: it closes the biggest promise/reality gap, removes a sharp bean-creation error, and makes the
   library actually batteries-included on cloud-native runtimes.
2. **Sample README as the canonical 5-minute quickstart** (improvement #2). Converts the "~15 min"
   claim from assertion to proof, with zero new code, and gives every reader a clone-run-curl path.
3. **First-class Art.15 accuracy hook** (improvement #3). Lifts the newest feature to the same
   enforcement tier as the rest of the library, so "all required articles enforced" stops having an
   asterisk.

These three turn the library from "DX-first core wrapped in some manual edges" into "uniformly
batteries-included," which is the remaining distance to the Laravel bar.
