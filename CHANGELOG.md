# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html) once it leaves 0.x.

## [Unreleased]

### Docs
- Distribution policy revised: Maven Central is **not planned** for this repo. Updated [ADR-0005](docs/adr/0005-jitpack-distribution-v1.md) status from "deferred" to "not planned by design", with the framing that this is a reference / portfolio asset, not a commercial product, and the permanent maintenance cost of a Maven Central pipeline is not justified absent an adopter requiring it. README "Quick start" and "Roadmap" sections updated to make the position explicit upfront.

## [1.1.0] - 2026-05-02

API cleanup release. No new features. Sharpens the surface that 1.0.0 froze
where a senior code review surfaced smells worth fixing before the first
external adopter.

### Fixed
- **`PayloadHasher` no longer falls back to `System.identityHashCode`** when
  Jackson refuses a payload. The previous fallback was non-deterministic across
  JVM runs, which silently broke chain verification for any record whose input
  serialisation happened to throw. The new fallback is the deterministic marker
  `unserializable:<fully-qualified-type-name>`. Hashes for routine payloads are
  unchanged.

### Changed (BREAKING wire format on /aiact/log/head response field names, but the snake_case form is preserved)
- **`GET /aiact/log/head`** now returns the typed
  `AuditLogService.ChainHead(systemId, headHmac)` record instead of a raw
  `Map<String, String>`. JSON output keys remain `system_id` and `head_hmac`
  via `@JsonProperty`, so existing clients that parsed the JSON by field name
  continue to work. Java callers that consumed the controller method as
  `Map<String, String>` must switch to `ChainHead`. The endpoint also now
  routes through the new `AuditLogService.head(String)` API instead of doing
  a `Stream` walk with an `AtomicReference` inside the controller.
- New API: `AuditLogService.head(String systemId)` returning `ChainHead`.
  Default implementation walks the stream; concrete sinks that can answer in
  O(1) should override.

## [1.0.0] - 2026-05-02

First stable release. API freeze for `@AiAct*` annotations, `AuditLogService`, `AiActEndpointGuard`, `aiact.*` configuration properties and the `/aiact/**` REST shape. Breaking changes from here on out follow semantic versioning.

JitPack coordinates: `com.github.iambilotta.spring-aiact:spring-aiact-spring-boot-starter:v1.0.0`.

### Changed (BREAKING for users who relied on `@Qualifier("aiActObjectMapper")`)
- The internal `ObjectMapper` used to serialise Article 12 audit records is no longer
  exposed as a Spring bean. It is now built inside `AiActAutoConfiguration` and passed
  directly to the components that need it (`PayloadHasher`, `NdjsonAuditLogService`).
  Migration: applications that overrode hashing or NDJSON serialisation by registering
  their own `aiActObjectMapper` bean must now provide a `PayloadHasher` and/or an
  `AuditLogService` bean instead. The bean was undocumented as an extension point in
  v0.1.x, so most users will not be affected. `AiActLogController` no longer takes an
  `ObjectMapper`; it streams Article 12 exports through the new
  `AuditLogService.writeJsonLine(Writer, AuditEvent)` default method, which the
  NDJSON implementation overrides.
- This refactor closes [#2](https://github.com/iambilotta/spring-aiact/issues/2),
  the follow-up to the v0.1.1+ minimal fix that used `defaultCandidate=false`.

### Fixed
- `aiActObjectMapper` no longer shadows the application's primary `ObjectMapper`.
  Symptom before the v0.1.1 patch (PR #1) and now closed at the architectural level by
  this refactor: any user POST with camelCase JSON returned `400 Unrecognized field`
  because aiact's `SNAKE_CASE` mapper had taken over Spring MVC's deserialisation.
  Regression pinned by `ObjectMapperIsolationTest` (no `aiActObjectMapper` bean,
  exactly one `ObjectMapper` in the context, and that one is Spring Boot's primary).
  Hash determinism pinned by `PayloadHasherDeterminismTest`.

## [0.1.1] - 2026-05-01

Fix-only re-tag. v0.1.0 was tagged but the JitPack build failed because JitPack
ships Maven 3.0.5 by default, which is incompatible with `maven-compiler-plugin`
3.13.0. v0.1.1 adds the Maven Wrapper (`./mvnw` 3.9.15) and a `jitpack.yml` that
uses it, so the build is reproducible on JitPack, in CI, and on a contributor's
laptop without a system Maven install. v0.1.0 is deprecated; use 0.1.1 or later.

JitPack coordinates carry the `v` prefix because JitPack uses the git tag
verbatim as the Maven version: `com.github.iambilotta.spring-aiact:spring-aiact-
spring-boot-starter:v0.1.1`. The Maven Central future coordinates will drop the
`v` (`com.iambilotta.spring:spring-aiact-spring-boot-starter:0.1.1`).

### Added
- Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`).
- `jitpack.yml` that pins JDK 21 and invokes `./mvnw install` with the sample
  module excluded.

### Fixed
- JitPack build failure on v0.1.0 caused by the obsolete Maven version on the
  JitPack worker.

## [0.1.0] - 2026-05-01  (DEPRECATED, do not use)

Tagged but unbuildable on JitPack. Use [0.1.1] or later. Surface unchanged.

First publishable cut. Distributed via JitPack as
`com.github.iambilotta:spring-aiact-spring-boot-starter` until Maven Central
publication lands.

### Added
- `AiActEndpointGuard` SPI with `DenyAllAiActEndpointGuard` (default) and
  `AllowAllAiActEndpointGuard` (opt-in via `aiact.endpoints.allow-without-guard=true`). Every
  REST endpoint refuses calls until the deployer registers a real guard.
- `AiActMockEndpointGuard` test helper (production source, no test-jar overhead) with a
  fluent builder for allow/deny rules per (system id, action). Lets a consumer write
  integration tests against the spring-aiact endpoints without standing up Spring Security.
- Fail-fast on the default HMAC secret (`change-me-please`) in non-development profiles.
  Configurable via `aiact.hmac.fail-on-default-in-prod` and `aiact.hmac.development-profiles`.
- `MetadataSanitizer` that whitelists keys, truncates values at 256 chars, and replaces raw
  exception messages with a SHA-256 fingerprint to keep PII out of audit metadata.
- Multi-process safe append: every audit write acquires an OS-level `FileLock` and tails the
  file under the lock to recompute the chain head from disk. Configurable via
  `aiact.audit.single-writer-lock` (default `true`). Required in Kubernetes deployments
  where more than one pod writes to the same NDJSON file.
- `HighRiskAnnotationValidator` extracted from `VerifyMojo` for unit testability. Fixes a bug
  where any `@AiActDataset` anywhere on the classpath satisfied every high-risk system; the
  search is now scoped to the same package.
- `additional-spring-configuration-metadata.json` for IDE completion on every `aiact.*`
  property with descriptions, defaults and hint values.
- `AiActStartupReporter` logs a single structured INFO line at `ApplicationReady` time with
  the active configuration (`endpoints status`, `multi-process`, `retention`, `hmac status`,
  `log-dir`). Two warnings follow when the default HMAC secret or the unsafe permit-all
  guard are detected.
- Actionable error messages: deny reasons in 403 responses and validation exceptions in
  `OversightService` now include the configuration key or field that the caller should
  change, plus a documentation pointer where applicable.
- `AiActHealthIndicator` exposes `/actuator/health/aiact`. UP/DOWN reflects log directory
  writability and HMAC secret status; the details map carries `last-append-system`,
  retention, multi-process flag, endpoint enablement. Auto-configured only when Spring Boot
  Actuator is on the classpath (optional dependency).
- `examples/docker-compose/` end-to-end runnable demo: sample app + Caddy reverse proxy with
  basic auth + persistent volume. Includes a tamper test that walks through editing the
  NDJSON on disk and watching `/aiact/log/verify` flag the mismatch.
- 41 new tests across the AOP advisor, retention pruning, oversight service, audit export
  packager, validator, mock guard, and HMAC fail-fast. Total 56 tests, up from 18 at the
  scaffolding commit.
- `docs/PRODUCTION.md` with eight sections covering HMAC secret outside the source tree,
  `AiActEndpointGuard` wiring (Spring Security and OPA examples), the multi-pod / shared-FS
  support matrix with a `flock` verification recipe, the HMAC key rotation playbook, the
  retention export workflow, observability, the CI gate snippet, and what the deployer
  still owns under the AI Act regardless of the starter.
- GitHub Actions CI workflow with a JDK 21 + JDK 25 build matrix and a PR-only OWASP
  Dependency-Check job.
- `SECURITY.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `renovate.json`, OWASP Dependency-Check
  Maven profile (`-Pdependency-check`).

### Changed
- README rewritten UX/DX-first: TL;DR in three lines, status badge for the alpha state,
  comparison table vs Sprinto / Centraleyes / Credo AI / DIY logger, ASCII flow diagram,
  numbered quick-start with per-step time stamps, FAQ section anticipating the seven most
  common objections, REST endpoint table with the matching `Guard.Action` enum value.
- `NdjsonAuditLogService` constructor takes an explicit `multiProcessSafe` flag. The
  legacy single-arg constructor still works and now defaults to the safe path.
- `OversightService` gains a constructor accepting `MetadataSanitizer`. The legacy single-arg
  constructor wires a default sanitizer.
- `ClasspathScanner` no longer swallows `Throwable` silently; the failure is logged at debug
  level so `mvn -X` surfaces classes that could not be loaded for annotation scanning.

### Removed
- Dead dependencies `spring-boot-starter-jdbc` and `com.h2database:h2` from
  `spring-aiact-core`. They were declared but never used; the future JDBC sink will land in
  a separate optional module.

### Security
- Audit endpoints are now deny-by-default. Previously they were unauthenticated, which would
  have allowed any caller able to reach the application port to read the full Article 12
  audit log and submit fake oversight overrides.
- The HMAC secret guard prevents shipping to production with the placeholder secret.
- `MetadataSanitizer` blocks raw exception messages from reaching the audit log, closing a
  PII leak path the previous implementation left open.

### Documentation
- README links to `CHANGELOG.md`, `SECURITY.md`, `docs/PRODUCTION.md`, `CONTRIBUTING.md` and
  `examples/docker-compose/`. Each link is referenced from the section that needs it, not
  collected at the bottom.

_See git log between `dbd0ea0` (initial scaffolding) and the `v0.1.0` tag for the full delta._
