# Requirements | spring-aiact (the library's own to-be)

> **Layer**: governance / **to-be** (EARS, hand-written). This file declares what
> `spring-aiact` *must do* as a library: the contract an adopter relies on. The **as-is**
> (what the code does *right now*) is the source of truth and lives in the tests
> (`*Test.java` / `*IT.java`) and the ADRs under `docs/adr/`; the **gap between the two IS
> the backlog**. Sibling regulation library: [`spring-gdpr`](https://github.com/iambilotta/spring-gdpr);
> the two compose on a service that scores personal data (see REQ-AIACT-010).
>
> These are the requirements of the **library**, not of any adopter system. "The system"
> below means *spring-aiact as deployed in an adopter's Spring Boot service*. The running
> example is `spring-aiact-sample` (`HiringScreener`).

## Context

`spring-aiact` turns AI Act evidence obligations into code-derived artifacts: annotate one
high-risk `@Service`, get an Article 12 tamper-evident audit chain at runtime plus an Annex IV
technical file, an Article 47 Declaration of Conformity and per-dataset datasheets at every
build. Apache 2.0, JitPack distribution, no SaaS, no data egress.

**As-is status (2026-06-08, verified against the clone)**: the library is mature and shipped
(v2.0.0, Spring Boot 4 line; v1.1.0 LTS frozen). 64 tests. What is **implemented** is traced
below to a real test or class; what is **proposed** is the declared to-be (the README "Roadmap"
and "Reality check" sections already name most of these honestly).

**Coverage today** (verified): Articles 10, 11+Annex IV, 12, 13, 14, 15 (declarative only), 47,
Annex VII. The annotation set models the **high-risk** band only (`AnnexIIICategory` is the eight
official Annex III points; `AiActHighRiskSystem` + 3 enforced companions). The four documented
gaps the README already concedes:

1. **Article 50 transparency** (chatbot / generated-content disclosure) — absent. The library
   targets high-risk providers, not limited-risk transparency. Building it is the first new
   capability (REQ-AIACT-002).
2. **Non-filesystem audit sink** — the only sink is `NdjsonAuditLogService` (one append-only
   file per `system_id`). Roadmapped: a **JDBC-backed** sink behind the existing
   `AuditLogService` SPI (REQ-AIACT-003). The SPI is already open to it (ADR-0002 says so).
3. **Risk classification beyond high-risk** — no `prohibited` (Art.5) / `limited` (Art.50) /
   `minimal` band, no GPAI (Art.51-55), no FRIA (Art.27). The library does one band well rather
   than all four shallowly (REQ-AIACT-001, -008, -009, -012).
4. **Article 15 enforcement** — `@AiActAccuracyMetric` is **declarative**: it records a measured
   value, it does not measure or gate on it (REQ-AIACT-006).

**Layer boundary**: these are requirements (the WHAT). Design choices (HMAC vs signature, NDJSON
vs DB, AOP advisor vs LTW, ObjectMapper not-a-bean) live in `docs/adr/`, not here. A requirement
that depends on a design decision points at the ADR.

**Out of scope, by design** (the README "Reality check" owns this list, not retracted here):
Article 9 risk-management system, Article 17 QMS, Article 71 EU-database registration, Article 72
post-market monitoring, Article 73 incident reporting. The library is the IT-side evidence layer;
the organisational process is the adopter's.

---

## Requirements

### REQ-AIACT-001 | High-risk system declared as machine-readable source of truth
- category: functional
- priority: M
- source: AI Act Art.6 + Annex III; ADR-0001 (annotations as source of truth)
- rationale: every downstream artifact (technical file, DoC, audit attribution, datasheets) is derived from one annotated class; if the declaration is not machine-readable, the dossier drifts from the code by the next sprint
- trace-to: `AiActHighRiskSystem` (`spring-aiact-core/.../annotation/AiActHighRiskSystem.java`) + `AnnexIIICategory`; build-time discovery `AnnotationModelCollector`; sample `HiringScreener`
- status: implemented

WHEN a Spring `@Service` is a high-risk AI system, the system SHALL let the developer declare it
with `@AiActHighRiskSystem` (id, name, Annex III category, intended purpose, provider) as the
single machine-readable source of truth for all generated evidence.

```gherkin
Given a class annotated @AiActHighRiskSystem with an AnnexIIICategory
When the build runs the codegen discovery
Then the system id, name and category are collected into the Annex IV technical file model
```

### REQ-AIACT-002 | Article 50 transparency disclosure
- category: functional
- priority: S
- source: AI Act Art.50 (limited-risk transparency)
- rationale: a user interacting with an AI system (chatbot, AI-generated content) has the right to be told so; this is the limited-risk band the library does not model today (gap #1). The README "Reality check" already concedes "not custom Annex III categories" and the high-risk scoping
- trace-to: none yet — disclosure mechanism (annotation `@AiActTransparency` + a response decorator / servlet filter that stamps the disclosure header/label) to be built; contributed as a new optional concern
- chi-paga-questo-nove: a new annotation + web concern; keep it out of `core` (no web dep) — it belongs in the starter or a new opt-in module
- status: proposed

WHEN an adopter system interacts with a natural person through an AI surface (chatbot, generated
content), the system SHALL provide a mechanism to disclose, clearly and machine-detectably, that
the interaction or content is AI-generated.

### REQ-AIACT-003 | Article 12 audit log on a non-filesystem (JDBC) sink
- category: technology-constraint
- priority: S
- source: AI Act Art.12 + Art.19; ADR-0002 (NDJSON default, SPI open to a DB sink)
- rationale: the default `NdjsonAuditLogService` writes one append-only file per system; on an ephemeral / scale-to-zero runtime (e.g. Cloud Run min0/max1) a filesystem sink does not persist. A JDBC-backed `AuditLogService` is the roadmapped answer (gap #2); the SPI already permits it
- trace-to: SPI `AuditLogService` (`spring-aiact-core/.../audit/AuditLogService.java`, `append`/`stream`/`verify`/`head`); default impl `NdjsonAuditLogService`. JDBC impl to be built as an optional module
- chi-paga-questo-nove: a new module + schema + an IT proving the HMAC chain survives across a DB round-trip; the SPI cost is already paid
- status: proposed

WHEN an adopter cannot rely on a durable local filesystem, the system SHALL offer an
`AuditLogService` implementation that persists the append-only HMAC-chained log to a relational
store, preserving the same tamper-evidence property as the NDJSON default.

### REQ-AIACT-004 | Tamper-evident append-only audit log (Article 12)
- category: qos-constraint
- priority: M
- source: AI Act Art.12; ADR-0002, ADR-0004 (HMAC chain over digital signatures)
- rationale: Article 12 is the load-bearing claim of the whole library — if a record can be silently edited or deleted the chain is worthless; tamper must be *detectable* by a third party with the key
- trace-to: `HmacChain` + `HmacChainTest#verifyDetectsTampering`, `#chainPropagatesPrevHmac`; `NdjsonAuditLogServiceTest#verifyDetectsTamperingOnDisk`; multi-writer safety `NdjsonMultiWriterTest#chainStaysValidWithFileLockEnabledAcrossTwoConcurrentWriters`
- chi-paga-questo-nove: `single-writer-lock=true` + fsync per append caps single-writer throughput (~180 ops/s on local SSD, JMH); the README documents the trade and points high-throughput adopters at single-writer-process mode or the future JDBC sink
- status: implemented

WHEN a high-risk method annotated `@AiActLog` is invoked, the system SHALL append one record to
an append-only log, chaining each record's HMAC over the canonical record plus the previous
record's HMAC, such that any edit, reorder or deletion is detectable via `/aiact/log/verify`.

```gherkin
Given a verified NDJSON audit log for a system
When one byte of any past record is altered on disk
Then GET /aiact/log/verify reports that record (and every record linking back to it) as invalid
```

### REQ-AIACT-005 | Article 14 human oversight, override recorded as a linked event
- category: functional
- priority: M
- source: AI Act Art.14
- rationale: an Article 14 override must be attributable to a natural person and linked to the original inference; it is itself an audit fact, never a mutation of the original record
- trace-to: `@AiActOversight` + `OversightLevel`; `OversightService#recordOverride` (links via `linkedEventId`, appends a second event); `OversightServiceTest#recordsAcceptAsOverrideKind`, `#rejectsMissingActor`, `#rejectsMissingLinkedEventIdForNonStopDecision`
- status: implemented

WHEN a human reviews and overrides a high-risk AI output, the system SHALL record the override as
a second audit event linked to the original inference, carrying the actor and decision, and SHALL
reject an override missing the actor or (for non-`stop` decisions) the linked event.

```gherkin
Given an original inference audit event with an event id
When an HR specialist submits an Article 14 override referencing that event id
Then a second linked override event is appended with the actor and decision recorded
```

### REQ-AIACT-006 | Article 15 accuracy/robustness metric — measured, not just declared
- category: qos-constraint
- priority: S
- source: AI Act Art.15
- rationale: today `@AiActAccuracyMetric` is **declarative** — it records a value an adopter measured elsewhere and forwards it to the technical file; it does not compute or gate on it (gap #4, the annotation javadoc says so). The to-be is enforcement: fail or flag when a measured metric drops below the declared threshold
- trace-to: implemented (declarative): `AiActAccuracyMetric` annotation forwarded to the technical file accuracy section. Enforcement: none yet — an eval hook / build assertion to be built
- chi-paga-questo-nove: the library cannot measure model accuracy; enforcement needs an adopter-supplied evaluation harness feeding a value the library can compare to the threshold
- status: proposed

WHEN a high-risk system declares an `@AiActAccuracyMetric` threshold and a measured value is
supplied, the system SHALL compare the value to the threshold and surface a failure when it
falls below.

### REQ-AIACT-007 | Annex IV technical file generated from annotations at build
- category: functional
- priority: M
- source: AI Act Art.11 + Annex IV
- rationale: the technical dossier of a high-risk system is derived from the code, not maintained by hand; empty annotations produce visible gap markers, not hallucinated text
- trace-to: `TechnicalFileMarkdownRenderer` + `TechnicalFileMarkdownRendererTest#rendersAllNineSectionsWithGapPlaceholdersWhenEmpty`, `#rendersTablesWhenSectionsArePopulated`; `GenerateMojo`
- status: implemented

WHEN the project is built, the system SHALL generate a nine-section Annex IV Markdown technical
file from the annotations, emitting explicit gap placeholders for sections without declared data.

### REQ-AIACT-008 | Article 47 Declaration of Conformity generated at build
- category: functional
- priority: S
- source: AI Act Art.47
- rationale: the DoC is part of the regenerable dossier; it moves with the code like the technical file
- trace-to: `DeclarationOfConformityPdfGenerator` + `DeclarationOfConformityPdfGeneratorTest#rendersValidPdfHeader`
- status: implemented

WHEN the project is built, the system SHALL generate an Article 47 Declaration of Conformity PDF
with a signature placeholder.

### REQ-AIACT-009 | Article 10 per-dataset datasheet generated at build
- category: functional
- priority: S
- source: AI Act Art.10 + Art.13 (instructions for use)
- rationale: dataset provenance, size, license and known biases are part of the dossier; declared once on `@AiActDataset`, rendered per dataset
- trace-to: `@AiActDataset` + `@AiActIntendedPurpose`; `DatasetDatasheetRenderer` + `DatasetDatasheetRendererTest#rendersBiasesWhenDeclared`, `#rendersGapWhenBiasesAreEmpty`
- status: implemented

WHEN a high-risk system declares one or more `@AiActDataset`, the system SHALL generate a Markdown
datasheet per dataset (source, size, license, declared biases) at build time.

### REQ-AIACT-010 | Companion annotations enforced at build (fail-fast completeness)
- category: technology-constraint
- priority: M
- source: AI Act Annex IV completeness; ADR-0001
- rationale: a high-risk declaration without its required companions yields an incomplete dossier; the gap must fail the build, naming the missing companion, not pass silently
- trace-to: `HighRiskAnnotationValidator#validate` (names missing `@AiActIntendedPurpose`/`@AiActOversight`/`@AiActDataset`); `HighRiskAnnotationValidatorTest#missingPurposeIsReported`, `#missingDatasetIsReportedEvenWhenAnotherSystemHasOneInADifferentPackage`, `#compliantClassPassesValidation`; `VerifyMojo`
- status: implemented

WHEN a class carries `@AiActHighRiskSystem` but is missing a required companion annotation, the
build SHALL fail and name the missing companion and the Article it satisfies.

```gherkin
Given a class annotated @AiActHighRiskSystem without @AiActIntendedPurpose
When the spring-aiact-maven-plugin verify goal runs
Then the build fails reporting "missing @AiActIntendedPurpose (Article 13)"
```

### REQ-AIACT-011 | Audit endpoints deny-by-default behind a pluggable guard
- category: qos-constraint
- priority: M
- source: AI Act Art.12 confidentiality; SECURITY.md threat model (Information disclosure)
- rationale: the audit log must not be readable by an unauthorised caller; the library does not depend on Spring Security, so the guard is an SPI any auth stack plugs into, denying by default in production
- trace-to: `AiActEndpointGuard` SPI + `DenyAllAiActEndpointGuard`; `EndpointDenyByDefaultTest#exportEndpointReturns403WhenNoGuardConfigured`, `#verifyEndpointReturns403WhenNoGuardConfigured`, `#headEndpointReturns403WhenNoGuardConfigured`
- status: implemented

WHEN a caller reaches any `/aiact/log/**` or `/aiact/oversight/**` endpoint, the system SHALL
route the call through the configured `AiActEndpointGuard` and deny by default when no guard is
wired in a production profile.

### REQ-AIACT-012 | Risk classification beyond the high-risk band (prohibited / limited / minimal / GPAI)
- category: technology-constraint
- priority: C
- source: AI Act Art.5 (prohibited), Art.50 (limited), Art.51-55 (GPAI)
- rationale: today the model declares only the high-risk band; a fuller classification (reject Art.5 prohibited practices by construction, mark limited-risk for Art.50, flag GPAI) is the to-be (gap #3). Deliberately deprioritised: the library does one band well (README "Reality check"), this is breadth not depth
- trace-to: none yet — would extend the annotation model with a risk-class dimension and a build gate that refuses the `prohibited` class; cross-ref REQ-AIACT-002 (limited-risk transparency)
- status: proposed

WHEN a developer classifies an AI system, the system SHALL allow declaring its risk class
(prohibited / high-risk / limited / minimal, plus GPAI) and SHALL refuse, by construction, a
system classified as a prohibited Article 5 practice.

### REQ-AIACT-013 | Fundamental Rights Impact Assessment scaffold (Article 27)
- category: functional
- priority: W
- source: AI Act Art.27
- rationale: some high-risk deployers must produce a FRIA; the library generates the technical file and DoC from annotations, so a FRIA scaffold is a natural sibling (gap #3). Won't-this-time: no adopter demand yet, and it overlaps the deployer's process more than the provider's code
- trace-to: none — a `fria.md` scaffold generated from the annotation model, sibling of the DPIA scaffold in `spring-gdpr`
- status: proposed

WHEN a high-risk system is deployed in a context that requires a FRIA, the system SHALL generate
a FRIA scaffold from the declared annotation model, to be completed by the deployer.

### REQ-AIACT-014 | Annex VII evidence pack (signed export for a notified body)
- category: functional
- priority: S
- source: AI Act Annex VII
- rationale: the audit slice plus generated dossier must be exportable as one tamper-evident bundle a notified body can verify
- trace-to: `AuditExportPackager` + `AuditExportPackagerTest#packageContainsAllEntriesPlusSignedManifest`, `#manifestHmacVerifiesAgainstTheManifestText`
- status: implemented

WHEN an adopter prepares a notified-body submission, the system SHALL package the audit export
plus generated documents into a ZIP with an HMAC-signed manifest that verifies against its text.

### REQ-AIACT-015 | Declared retention applied to the audit log (Article 12 lifetime)
- category: qos-constraint
- priority: S
- source: AI Act Art.12 (logs kept over the system lifetime) + Art.19
- rationale: the log is kept for a declared horizon then pruned; pruning must keep the kept slice verifiable (the documented boundary trade-off), never corrupt it
- trace-to: `RetentionPolicyService#prune` + `RetentionPolicyServiceTest#prunesRecordsOlderThanCutoff`, `#chainStaysVerifiableForKeptSliceAfterPrune`
- chi-paga-questo-nove: a prune leaves one verifier "false positive" at the boundary record (kept slice carries the deleted predecessor's `prev_hmac`); documented in the test and the README, export-before-prune is the adopter's procedure
- status: implemented

WHEN audit records exceed the configured retention horizon, the system SHALL prune them while
keeping the retained slice independently verifiable.

### REQ-AIACT-016 | No PII leaks into audit metadata (information-disclosure chokepoint)
- category: qos-constraint
- priority: M
- source: AI Act Art.12 + SECURITY.md threat model (Information disclosure, High); GDPR Art.5(1)(f)
- rationale: audit metadata is a silent PII leak vector; a single sanitizer is the chokepoint, whitelisting keys and bounding values, and exception fingerprints must never carry the message
- trace-to: `MetadataSanitizer` + `MetadataSanitizerTest#dropsKeysOutsideTheWhitelist`, `#truncatesValuesAtTheConfiguredMaxLength`, `#describeExceptionEmitsClassNameAndFingerprintNotMessage`
- status: implemented

WHEN audit metadata is recorded, the system SHALL drop keys outside the configured whitelist,
truncate values at the configured limit, and emit exception class + fingerprint rather than the
exception message.

### REQ-AIACT-017 | Fail-fast on the default HMAC secret in production
- category: qos-constraint
- priority: M
- source: AI Act Art.12 integrity; SECURITY.md threat model (Spoofing)
- rationale: a tamper-evident chain keyed on the placeholder default is forgeable; the starter must refuse to boot in a non-dev profile with the default secret
- trace-to: `HmacFailFastTest#failsToStartWhenSecretIsDefaultAndNoDevProfile`, `#startsWhenDevProfileActive`, `#startsWhenSecretOverridden`
- status: implemented

WHEN the starter boots in a non-development profile with the placeholder HMAC secret, the system
SHALL refuse to start.

### REQ-AIACT-018 | Audit record hashing is deterministic
- category: qos-constraint
- priority: M
- source: AI Act Art.12 verifiability; ADR-0006 (internal ObjectMapper not a bean)
- rationale: verification recomputes the HMAC over the record's SHA-256 input; if hashing is not deterministic, a clean log fails verify. The internal ObjectMapper is isolated from the application context precisely so an adopter's Jackson config cannot perturb it
- trace-to: `PayloadHasherDeterminismTest#sameJsonPayloadAlwaysHashesToTheSameValue`, `#hashesDifferOnDifferentPayloads`; `ObjectMapperIsolationTest#aiActDoesNotPublishAnInternalObjectMapperBean`
- status: implemented

WHEN the same logical payload is hashed, the system SHALL produce the same SHA-256 value
regardless of the adopter's application Jackson configuration.

### REQ-AIACT-019 | Data protection on AI systems that process personal data (cross-ref spring-gdpr)
- category: technology-constraint
- priority: S
- source: AI Act + GDPR (training/inference data is frequently personal data)
- rationale: a high-risk AI system that processes personal data inherits the full GDPR obligation set; the two regulation libraries compose rather than overlap
- trace-to: sibling library [`spring-gdpr`](https://github.com/iambilotta/spring-gdpr); cross-library demo `spring-gdpr-aiact-demo`
- status: proposed

WHEN an adopter system processes personal data through a high-risk AI surface, the system SHALL
let the adopter satisfy GDPR obligations via `spring-gdpr` on the same evidence-as-code
foundation, on top of the AI Act obligations here.

---

## Traceability

Every `implemented` requirement above cites a test method or class verified in this clone; the
test is the as-is spec, this file is the to-be contract, and the `proposed` set is the backlog.
A later run of **tracegate** (the as-built catalog generator used in the housetree monorepo) can
derive the REQ ↔ test matrix from the `@spec.*`-style mapping; this file declares the to-be it
will be checked against. Tracegate is **not** wired into this repo yet — when it is, it reads the
test javadoc, it does not edit this file.

## Changelog
- 2026-06-08 | REQ-AIACT-001..019 | created | canonical EARS to-be layer for the library's own requirements; `implemented` traced to verified tests/classes in the clone, `proposed` = the four declared gaps (Art.50 transparency, JDBC sink, risk-class beyond high-risk incl. GPAI/FRIA, Art.15 enforcement); sibling of spring-gdpr; English to match the public OSS repo convention
