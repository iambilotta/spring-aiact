# spring-aiact

> EU AI Act compliance-by-annotation for Spring Boot. Evidence-as-code for the Annex IV
> technical file, the Article 12 audit log, the Article 47 declaration of conformity and the
> Article 14 human oversight events.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5%2B-green.svg)](https://spring.io/projects/spring-boot)

## Why this exists

The EU AI Act (Regulation (EU) 2024/1689) requires high-risk AI systems to ship with:

- a technical file (Article 11, Annex IV),
- an event log (Article 12),
- a declaration of conformity (Article 47, Annex V),
- documented human oversight (Article 14),
- accuracy and robustness commitments (Article 15),
- governance of training data (Article 10).

Most teams paste together a SaaS GRC tool (Sprinto, Centraleyes, Credo AI Readiness Pack) plus
a Confluence dump that drifts away from the code. **`spring-aiact` flips the relationship**:
the code is the source of truth, the documentation is a build artifact regenerated from
annotations on every CI run.

The window of attention is the AI Act high-risk obligations deadline of **2 August 2026**.

## What it ships

| Module | Purpose |
|---|---|
| `spring-aiact-core` | Annotation set, AOP advisor, Article 12 NDJSON+HMAC audit log, retention. |
| `spring-aiact-codegen` | Build-time generators: Annex IV technical file MD, Article 47 DoC PDF, dataset datasheets, Annex VII export package. |
| `spring-aiact-spring-boot-starter` | Auto-configuration, REST endpoints (`/aiact/log/*`, `/aiact/oversight/*`), retention scheduler. |
| `spring-aiact-maven-plugin` | `verify` mojo (fails the build on missing companion annotations) and `generate` mojo (emits the artifacts under `target/generated-docs`). |
| `spring-aiact-sample` | Working example: a fake `HiringScreener` annotated as Annex III.4 high-risk, with an integration test that asserts the HMAC chain is valid. |

## Quick start

Add the starter to your Spring Boot 3.5+ application:

```xml
<dependency>
    <groupId>com.iambilotta.spring</groupId>
    <artifactId>spring-aiact-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Annotate the entry point of your high-risk system:

```java
@Service
@AiActHighRiskSystem(
    id = "hiring-screener",
    name = "Hiring screener",
    category = AnnexIIICategory.EMPLOYMENT_AND_WORKERS_MANAGEMENT,
    annexSubpoint = "4(a)",
    intendedPurpose = "Score CV applicants for an engineering role.",
    provider = "ACME"
)
@AiActIntendedPurpose(
    deploymentContext = "HR triage before human review.",
    users = {"HR specialists"},
    foreseeableMisuse = {"Auto-rejection without human review"}
)
@AiActOversight(
    level = OversightLevel.HUMAN_IN_THE_LOOP,
    description = "Every output reviewed by HR.",
    overrideRole = "hr"
)
@AiActDataset(
    id = "cv-2025", name = "Anonymized CV corpus 2025", phase = "training",
    source = "internal-s3://cv-2025", size = "12,500", license = "internal",
    biases = {"under-representation of women in STEM"},
    personalData = true
)
public class HiringScreener {

    @AiActLog(modelId = "hiring-screener@0.0.1")
    public ScoringResult score(CandidateApplication application) {
        // ...
    }
}
```

Configure the secret used for the audit log HMAC chain:

```yaml
aiact:
  hmac:
    secret: ${AIACT_HMAC_SECRET}
  log-dir: /var/log/aiact
  retention: P10Y
  endpoints:
    enabled: true
    base-path: /aiact
```

Wire the Maven plugin in the build:

```xml
<plugin>
    <groupId>com.iambilotta.spring</groupId>
    <artifactId>spring-aiact-maven-plugin</artifactId>
    <version>0.1.0</version>
    <executions>
        <execution>
            <goals>
                <goal>verify</goal>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Articles covered (v0.1)

| Article | What spring-aiact does |
|---|---|
| Article 10 | `@AiActDataset` declarations + per-dataset Markdown datasheets. |
| Article 11 + Annex IV | Build-time technical file generator (9 sections, Markdown). |
| Article 12 | NDJSON append-only audit log with HMAC chain. Tamper detection via `/aiact/log/verify`. |
| Article 13 | `@AiActIntendedPurpose` populates the Instructions for Use section. |
| Article 14 | `@AiActOversight` declarations + REST oversight endpoint that records overrides as second events. |
| Article 15 | `@AiActAccuracyMetric` declarations forwarded to the technical file. |
| Article 47 | Article 47 Declaration of Conformity PDF with signature placeholder. |
| Annex VII | `AuditExportPackager` produces a signed ZIP for notified body submission. |

## REST endpoints

When `aiact.endpoints.enabled=true` (default) the starter exposes:

- `GET /aiact/log/export?system={id}&from={iso}&to={iso}` returns NDJSON.
- `GET /aiact/log/verify?system={id}&from={iso}&to={iso}` returns the chain verification report.
- `GET /aiact/log/head?system={id}` returns the current chain head HMAC.
- `POST /aiact/oversight/{eventId}/override` records an Article 14 override.

## Anti-patterns explicitly out of scope

- **Not a wrapper around Logback.** Without the Annex IV generator, this would be an audit
  logger, not an AI Act compliance tool.
- **No custom Annex III categories.** The enum reflects the eight official Annex III points and
  their sub-points. If the regulation is amended, the enum is amended; never invent values.
- **No certification claim.** Only a notified body certifies. `spring-aiact` produces the
  evidence pack you submit to one. Keep that distinction visible everywhere you sell or use it.
- **Not a SaaS replacement for governance.** The annotations are a delivery vehicle for what
  your data governance and risk management process already decided. Empty annotations produce
  visible gap markers, not hallucinated text.

## Roadmap

- v0.1 (June 2026, target): the surface above. Apache 2.0, Maven Central.
- v0.5 (July 2026): build-time `verify` integrated with CI templates (GitHub Actions, GitLab,
  Jenkins). 1+ contribution upstream to Spring AI / `mcp-security`.
- v1.0 (December 2026): retention encryption-at-rest defaults, JDBC-backed audit sink,
  multi-tenant isolation patterns documented.

## License

Apache License, Version 2.0. See [LICENSE](LICENSE).

## Maintainer

Built and maintained by [Francesco Bilotta](https://iambilotta.com)
([@iambilotta](https://github.com/iambilotta)). Contact: francesco@iambilotta.com.
