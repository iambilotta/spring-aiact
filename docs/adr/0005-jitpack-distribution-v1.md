# ADR-0005: JitPack as the canonical distribution; Maven Central not planned

- **Status:** accepted (revised 2026-05-02 from "deferred" to "not planned by design")
- **Date:** 2026-04-30 (initial), 2026-05-02 (revision)
- **Deciders:** Francesco Bilotta

## Context

A Java library has three realistic distribution channels:

- **JitPack**: builds the artifact on first request from a git tag. No Sonatype account, no GPG signing required. The JitPack-side groupId is `com.github.<user>.<repo>` (rewritten), the version is the git tag verbatim (with the `v` prefix preserved).
- **Maven Central via Sonatype Central**: the default Java ecosystem channel. Requires a Sonatype account, namespace verification (often DNS TXT), GPG signing of every artifact, and signed releases via a CI pipeline.
- **GitHub Packages**: requires consumers to authenticate with a GitHub token. Friction for adopters.

The library is in v1.x with the goal of being adoptable but the maintainer's bandwidth is limited.

## Decision

JitPack ships in five minutes; Sonatype namespace verification takes 5-15 days wall-clock plus permanent ongoing maintenance. v1.0 was tagged within a one-day shipping window and JitPack was the realistic option. After the v1.x deep-investment cycle (v1.0 → v1.1 + ADRs + JMH harness + threat model + demo) the maintainer revised the position: **Maven Central is not planned for this repo**. This is a reference / portfolio asset of the maintainer, not a commercially supported product, and the cost of a Maven Central pipeline (immutable releases, GPG key custody, Sonatype workflow) is permanent. JitPack covers the consumer use case at zero ongoing cost.

Coordinates published in the README:

```
com.github.iambilotta.spring-aiact:spring-aiact-spring-boot-starter:v1.1.0
```

If a real adopter ever explicitly requires Maven Central (corporate policy blocking JitPack as a third party, security review constraint), the migration path is documented and the `release.yml` scaffolding is already wired. Until that trigger, the maintainer does not pay the maintenance tax.

## Consequences

- Adopters can pin a tag immediately. No registration, no token.
- The Maven plugin (`spring-aiact-maven-plugin`) cannot be consumed via JitPack because JitPack rewrites the plugin descriptor's groupId, and Maven rejects the inconsistency. The README points adopters who need build-time generation at a `mvn install` of the source tree, plus a profile activation in their consumer pom. Documented in the demo repo's README.
- The JitPack signal is mid-tier compared to Maven Central. A senior reviewer evaluating a compliance library may treat JitPack-only as "side project". The `release.yml` workflow already exists in the repo, ready to flip to Maven Central on namespace approval.
- Adopters who cannot reach JitPack from corporate CI can still build from source and install to their local mirror.

## Alternatives considered

**Maven Central from v1.0.** Rejected for v1.0 because of the verification window. After v1.1 the position hardened: Maven Central is not planned at all. Reasons in "Why this matters" below.

**Maven Central as a future planned milestone.** Considered through v0.5 / v1.0 thinking. Revised: the maintenance tax is permanent and the marginal benefit over JitPack is incremental. The repo is a reference asset, not a commercial product, and the trigger for a Maven Central migration is "real adopter requirement", not "looks more professional".

**GitHub Packages.** Rejected: adopter token requirement is friction.

**Self-hosted Nexus.** Over-engineering for a single-maintainer reference repo.

## Why this matters

This repo is the maintainer's **portfolio asset**, not a commercial product. The honest framing: "I built this to demonstrate evidence-as-code patterns for AI Act compliance and to use as my own technical authority signal in conversations; if it ends up adopted by another team that is a bonus, not the primary goal." JitPack covers the consumer use case at zero ongoing cost. Maven Central imposes a permanent maintenance tax (immutable releases mean every typo is forever, GPG key rotation is painful, Sonatype security workflows demand attention). For a non-commercial reference repo, the math does not work until someone explicitly says they cannot consume from JitPack.

The principle generalises: **do not pay infrastructure-grade maintenance costs for an asset that does not have infrastructure-grade adoption demand**. JitPack at zero maintenance, Maven Central if and when an adopter asks.

## References

- The first JitPack release tag is `v0.1.1` (commit `b26cee2`). v0.1.0 build had failed on JitPack because of an outdated Maven version on JitPack's sandbox; the fix was the Maven Wrapper (commit `533f7a7`).
- Maven Central scaffolding (release.yml workflow, Sonatype Central plugin in pluginManagement) is already wired and waiting. The activation cost is real (GPG key generation, Sonatype namespace verification, four GitHub Secrets) and intentionally not paid.
- Trigger for revisiting: a real adopter explicitly states "we cannot consume from JitPack". Until then, the decision stands.
