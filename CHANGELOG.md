# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html) once it leaves 0.x.

## [Unreleased]

### Added
- `AiActEndpointGuard` SPI with `DenyAllAiActEndpointGuard` (default) and
  `AllowAllAiActEndpointGuard` (opt-in via `aiact.endpoints.allow-without-guard=true`). Every
  REST endpoint refuses calls until the deployer registers a real guard.
- Fail-fast on the default HMAC secret (`change-me-please`) in non-development profiles.
  Configurable via `aiact.hmac.fail-on-default-in-prod` and
  `aiact.hmac.development-profiles`.
- `MetadataSanitizer` that whitelists keys, truncates at 256 chars, and replaces raw exception
  messages with a SHA-256 fingerprint to keep PII out of the audit metadata.
- Multi-process safe append: every audit write acquires an OS-level `FileLock` and tails the
  file under the lock to recompute the chain head from disk. Configurable via
  `aiact.audit.single-writer-lock` (default `true`).
- `HighRiskAnnotationValidator` extracted from `VerifyMojo` for unit testability. Fixes a bug
  where any `@AiActDataset` anywhere on the classpath satisfied every high-risk system; the
  search is now scoped to the same package.
- 35+ new tests across the AOP advisor, retention pruning, oversight service, audit export
  packager and validator. Total 53 tests at this commit, up from 18.
- `docs/PRODUCTION.md` with Spring Security wiring, multi-pod caveats, key rotation playbook,
  retention export workflow.
- GitHub Actions CI workflow (matrix on JDK 21 and JDK 25), `SECURITY.md`, this `CHANGELOG.md`,
  Renovate config, OWASP Dependency-Check Maven profile.

### Changed
- `NdjsonAuditLogService` constructor takes an explicit `multiProcessSafe` flag. The
  legacy single-arg constructor still works and now defaults to the safe path.
- `OversightService` gains a constructor accepting `MetadataSanitizer`. The legacy single-arg
  constructor wires a default sanitizer.
- README documents the operational edges (multi-pod, retention chain seed gap, encryption
  status as v1.0 placeholder) instead of glossing over them.

### Removed
- Dead dependencies `spring-boot-starter-jdbc` and `com.h2database:h2` from
  `spring-aiact-core`. They were declared but never used; the future JDBC sink will land in a
  separate optional module.

### Security
- Audit endpoints are now deny-by-default. Previously they were unauthenticated, which would
  have allowed any caller able to reach the application port to read the full Article 12
  audit log and submit fake oversight overrides.
- The HMAC secret guard prevents shipping to production with the placeholder secret unnoticed.

## [0.1.0] - unreleased

Initial scaffolding. See git log between the `chore: bootstrap multi-module Maven build`
commit and the first release tag.
