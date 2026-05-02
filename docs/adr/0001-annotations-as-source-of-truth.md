# ADR-0001: Annotations as the source of truth for AI Act evidence

- **Status:** accepted
- **Date:** 2026-04-29
- **Deciders:** Francesco Bilotta

## Context

The EU AI Act requires the provider of a high-risk system, before placing it on the market, to keep an Annex IV technical file (Article 11), an Article 12 audit log of inferences, an Article 47 Declaration of Conformity, an Article 14 documented oversight contract, and per-dataset datasheets (Article 10). Every artifact must reflect the system as it actually is at the time of the conformity assessment.

The audits I have walked into share a failure mode: a `tech-file-2025.docx` last touched eight months ago, describing a model version no longer running. The information was already in the codebase. The tooling was missing.

Three places could conceivably hold this metadata: a YAML config shipped with the app, a SaaS governance dashboard configured by humans, or annotations on the Java class that implements the high-risk system.

## Decision

The annotations on the high-risk Spring `@Service` are the single source of truth for the AI Act evidence pack. The Maven plugin reads them at build time and writes the Annex IV technical file, the DoC PDF and the dataset datasheets. The Spring Boot starter reads them at runtime and decides what to log under Article 12 and how to attribute Article 14 oversight overrides. Both halves read the same Java symbols.

Concretely the eight annotations (`@AiActHighRiskSystem`, `@AiActIntendedPurpose`, `@AiActOversight`, `@AiActDataset`, `@AiActAccuracyMetric`, `@AiActLog`, plus the enums `AnnexIIICategory`, `OversightLevel`, `RiskMetric`) live in the `spring-aiact-core` module.

## Consequences

- Refactor the `HiringScreener` class, the next `mvn verify` regenerates the technical file with the updated content. A notified body assessor reviewing the dossier sees the architecture as it is.
- Remove `@AiActOversight`, the build fails with a precise message "missing required companion annotation". Adopting a high-risk class without all four companions is a compile-time error, not a runtime gap.
- The annotations live with the code. The version-controlled history that the company already audits also documents every change to the AI Act dossier.
- Caveat: this is the source of truth for the **dossier**, not the **claim**. `@AiActAccuracyMetric(threshold = ">=0.92")` finishes in the technical file regardless of whether the model actually meets that threshold; the accuracy *test* remains the engineering team's responsibility. The library is evidence assembly, not validation.

## Alternatives considered

**External YAML config under `src/main/resources/aiact.yaml`.** Detached from the Java class. A rename of the class does not move the YAML; the file drifts. Same failure mode as the `.docx` we are replacing.

**SaaS governance dashboard (Sprinto, Centraleyes, Credo AI).** Adopters pay 25k EUR/year, audit data leaves the on-prem boundary, dashboard configuration drifts from the running system. Net negative for our target.

**Spring `@ConfigurationProperties` shape.** Plausible but has the same drift problem as YAML at one remove: the binding is to the property tree, not to the class. Renaming the class leaves the property names intact.
