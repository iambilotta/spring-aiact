# CLAUDE.md — `spring-aiact` (repo working contract)

The technical working contract for writing code in this repo. Apache-2.0 OSS library that turns
EU AI Act evidence obligations into code-derived artifacts. Public asset of
[iambilotta.com](https://iambilotta.com); sister repo `spring-gdpr`. Docs, ADRs, README, commits,
comments are **English** (public OSS audience); no em-dashes.

## What the library is (one line)

Annotate one high-risk `@Service` once, get an Article 12 tamper-evident audit chain at runtime
plus an Annex IV technical file, an Article 47 DoC and per-dataset datasheets at every build. No
SaaS, no data egress. The annotated class is the **single source of truth** (ADR-0001).

## Where lives WHAT (use judgement, not a rote rule)

- **Code (comments)** = WHY of non-obvious **invariants** that survive refactors
  ("snake_case keys keep the SHA-256 input deterministic", "this principal must implement
  Serializable"). Not session chronicle, not decision narrative. Code is the clean **to-be**.
- **ADR** (`docs/adr/0NNN-*.md`, Nygard template `0000-template.md`) = locked design decisions
  (choice + why + alternatives rejected). Never deleted; superseded with cross-links.
- **`docs/requirements/aiact.requirements.md`** = the library's **to-be** EARS requirements
  (the WHAT the library must do). `implemented` traces to a real test; `proposed` is the backlog.
- **CHANGELOG.md** = released delta, Keep-a-Changelog style.
- **Commit message** = atomic delta + why; the architectural why points at the ADR.

## The two layers of truth (read this before editing)

- **As-is** (what the code does *now*) = the **tests** (`*Test.java` / `*IT.java`) + the ADRs.
  The test is the spec. To change behaviour, change a test first.
- **To-be** (what the library *must* do) = `docs/requirements/aiact.requirements.md`, EARS,
  hand-written. The **gap between the two IS the backlog** (the four declared gaps: Art.50
  transparency, JDBC sink, risk-class beyond high-risk incl. GPAI/FRIA, Art.15 enforcement).

## The law: no behaviour change without a failing test that demands it

The audit chain is the load-bearing claim of the whole library (ADR-0002). Discipline:

- **No production code without a failing test first.** Minimal code to GREEN, refactor only green.
- **Never weaken a tamper-evidence test** (`HmacChainTest`, `NdjsonAuditLogServiceTest`,
  `NdjsonMultiWriterTest`) to make a build pass. If verify breaks, the impl is wrong, not the test.
- **`single-writer-lock` semantics are load-bearing** for multi-pod correctness; the throughput
  cost is documented and accepted, not a bug to "optimise away".
- **The internal `ObjectMapper` is not a Spring bean** (ADR-0006): hashing determinism depends on
  it being isolated from the adopter's Jackson config. Pinned by `ObjectMapperIsolationTest`.

## Hard rules

- **Annotations are the source of truth** (ADR-0001). Generated evidence is derived, never hand-written;
  empty annotations produce visible gap markers, never hallucinated text.
- **No custom Annex III categories.** `AnnexIIICategory` mirrors the eight official Annex III points;
  it changes only when the regulation is amended. (CONTRIBUTING + README "Reality check" own this.)
- **`core` stays web-free and Spring-Security-free.** Auth plugs in through the `AiActEndpointGuard`
  SPI; new persistence backends implement `AuditLogService`, they do not fork it.
- **Audit endpoints deny by default** in production (`DenyAllAiActEndpointGuard`); never ship a path
  that reads `/aiact/log/**` without the guard.
- **No PII into audit metadata.** `MetadataSanitizer` is the chokepoint (whitelist keys, bound values,
  exception fingerprint not message). Bypassing it is a High security finding (SECURITY.md).
- English everywhere in the repo; no em-dashes.

## Scope discipline (the README "Reality check" is binding)

Out of scope, declared and not silently re-expanded: Article 9 risk-management system, Article 17
QMS, Article 71 EU-database registration, Article 72 post-market monitoring, Article 73 incident
reporting, and notified-body **certification** (the library is evidence-as-code, not a certifier).
A `proposed` requirement is a candidate, not a commitment; promotion to `implemented` happens only
when a test exists.

## Build entry points (multi-module reactor, no surprises)

```
sdk use java 21.0.11-amzn
mvn clean test            # fast: unit + slice across all 6 modules (sub-minute)
mvn verify                # full gate: + maven-plugin verify/generate goals, codegen ITs
./mvnw -pl spring-aiact-benchmark -am package   # JMH harness (perf claims are measured, not asserted)
```

Modules: `core` (annotations + audit primitives) · `codegen` (Markdown + PDF renderers) ·
`spring-boot-starter` (autoconfigure, advisor, REST) · `maven-plugin` (build-time verify+generate) ·
`sample` (runnable `HiringScreener`) · `benchmark` (JMH). Adopters import the starter only.

## Distribution

JitPack, dual line: `v2.x` (Spring Boot 4, active) / `v1.1.0` (Spring Boot 3.5+, LTS frozen).
Maven Central is **deliberately not planned** (ADR-0005): reference/portfolio asset, the release
pipeline cost is only worth paying on concrete adopter demand. Do not add it without that demand.

## Later: tracegate

The as-built REQ ↔ test catalog generator (used in the housetree monorepo) can later derive the
traceability matrix from the test javadoc. It is **not** wired here; when it is, it reads tests and
never edits `docs/requirements/*.md`. Until then, `trace-to` lines in the requirements file are the
manual matrix.
