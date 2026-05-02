# ADR-0005: JitPack as the canonical distribution for v1.x; Maven Central deferred

- **Status:** accepted
- **Date:** 2026-04-30
- **Deciders:** Francesco Bilotta

## Context

A Java library has three realistic distribution channels:

- **JitPack**: builds the artifact on first request from a git tag. No Sonatype account, no GPG signing required. The JitPack-side groupId is `com.github.<user>.<repo>` (rewritten), the version is the git tag verbatim (with the `v` prefix preserved).
- **Maven Central via Sonatype Central**: the default Java ecosystem channel. Requires a Sonatype account, namespace verification (often DNS TXT), GPG signing of every artifact, and signed releases via a CI pipeline.
- **GitHub Packages**: requires consumers to authenticate with a GitHub token. Friction for adopters.

The library is in v1.x with the goal of being adoptable but the maintainer's bandwidth is limited.

## Decision

JitPack ships in five minutes; Sonatype namespace verification takes 5-15 days wall-clock. v1.0 was tagged within a one-day shipping window and JitPack was the realistic option. v1.1 stays on JitPack because nothing has forced our hand to migrate yet.

Coordinates published in the README:

```
com.github.iambilotta.spring-aiact:spring-aiact-spring-boot-starter:v1.1.0
```

Maven Central publication is queued behind a Sonatype namespace request for `com.iambilotta.spring`; once the namespace is verified, the library publishes signed artifacts from a tag-driven `release.yml` workflow. JitPack will keep mirroring git tags for the v1.x series; v2.0 will publish to Maven Central first and JitPack second.

## Consequences

- Adopters can pin a tag immediately. No registration, no token.
- The Maven plugin (`spring-aiact-maven-plugin`) cannot be consumed via JitPack because JitPack rewrites the plugin descriptor's groupId, and Maven rejects the inconsistency. The README points adopters who need build-time generation at a `mvn install` of the source tree, plus a profile activation in their consumer pom. Documented in the demo repo's README.
- The JitPack signal is mid-tier compared to Maven Central. A senior reviewer evaluating a compliance library may treat JitPack-only as "side project". The `release.yml` workflow already exists in the repo, ready to flip to Maven Central on namespace approval.
- Adopters who cannot reach JitPack from corporate CI can still build from source and install to their local mirror.

## Alternatives considered

**Maven Central from v1.0.** Rejected for v1: the namespace verification window is 5-15 days wall-clock and the maintainer's bandwidth was on shipping the API surface. Deferred to a future minor without breaking changes.

**GitHub Packages.** Rejected: adopter token requirement is friction.

**Self-hosted Nexus.** Over-engineering for a single-maintainer project at this stage.

## Why this matters

We are constantly tempted to pre-optimise for "production-grade signal" before any adopter has shown interest. JitPack is the right call until a real adopter asks for Maven Central. Spending two weeks on Sonatype before the first user values discipline only on paper. The honest version of this ADR is "shipped now, will revisit when the demand exists".

## References

- The first JitPack release tag is `v0.1.1` (commit `b26cee2`). v0.1.0 build had failed on JitPack because of an outdated Maven version on JitPack's sandbox; the fix was the Maven Wrapper (commit `533f7a7`).
- Maven Central scaffolding (release.yml workflow, Sonatype Central plugin in pluginManagement) is already wired and waiting. See the `release` profile in `pom.xml`.
- This ADR will be revisited when a v1.x adopter explicitly asks for Maven Central.
