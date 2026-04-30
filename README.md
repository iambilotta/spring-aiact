# spring-aiact

> EU AI Act compliance-by-annotation for Spring Boot. Evidence-as-code for the Annex IV
> technical file, the Article 12 audit log, the Article 47 declaration of conformity and the
> Article 14 human oversight events.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5%2B-green.svg)](https://spring.io/projects/spring-boot)
[![CI](https://github.com/iambilotta/spring-aiact/actions/workflows/ci.yml/badge.svg)](https://github.com/iambilotta/spring-aiact/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/badge/maven--central-not%20yet%20published-lightgrey.svg)](https://central.sonatype.com/)

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

Configure the secret used for the audit log HMAC chain and wire your auth stack into the
endpoint guard (the default refuses every call):

```yaml
aiact:
  hmac:
    secret: ${AIACT_HMAC_SECRET}      # required: starter refuses to start without a real value
  log-dir: /var/log/aiact
  retention: P10Y
  audit:
    single-writer-lock: true          # required when more than one pod writes the same file
  endpoints:
    enabled: true
    base-path: /aiact
    # Production must register a custom AiActEndpointGuard bean. Setting the flag below
    # activates the unsafe permit-all guard for local development only.
    allow-without-guard: false
```

A working `AiActEndpointGuard` example backed by Spring Security is in
[`docs/PRODUCTION.md`](docs/PRODUCTION.md). The starter does not depend on Spring Security, so
deployers can plug in OPA, an API key check, or any other auth stack via the same SPI.

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

Every call passes through the configured `AiActEndpointGuard`. The default deny-all guard
returns `403` with the reason `no-guard-configured`. Wire your real guard, then retry.

## Operational notes the README will not let you skip

**Multi-pod deployment.** The audit log is one append-only NDJSON file per system id. With more
than one pod writing to the same file (Kubernetes ReadWriteMany volume, two JVMs on the same
host, etc), set `aiact.audit.single-writer-lock=true` (default). Each append acquires an OS
file lock, tails the file under the lock and recomputes the chain head from disk, so the chain
stays valid across writers. The cost is one fsync per append (~1-3 ms on local SSD, slower on
networked filesystems). On NFSv3 without lockd, `flock` is not reliable; prefer NFSv4 or a
single-writer deployment.

**Retention prunes the chain seed.** When `RetentionPolicyService` removes records older than
the configured horizon, the new first record carries its original `prev_hmac` (the HMAC of the
now-deleted predecessor). A verifier walking from `CHAIN_SEED` will see one mismatch on that
boundary record. This is by design: pre-cutoff verifiability is the deployer's responsibility,
through the export-before-prune workflow described in `docs/PRODUCTION.md`. Test
`RetentionPolicyServiceTest.chainStaysVerifiableForKeptSliceAfterPrune` pins this trade-off.

**Encryption at rest is a v1.0 placeholder.** The `AiActProperties.Encryption` configuration
section is reserved for the future implementation and currently does nothing. Until v1.0 lands,
rely on the underlying filesystem's encryption (LUKS, EFS-at-rest, EBS-encrypted) for at-rest
protection. Do not interpret `aiact.encryption.enabled=true` as live encryption.

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

- v0.1 (June 2026, target): the surface above. Apache 2.0, Maven Central. Single SPI for auth
  (`AiActEndpointGuard`), file-locked audit log, deny-by-default endpoints, fail-fast on the
  default HMAC secret in production.
- v0.5 (July 2026): build-time `verify` integrated with CI templates (GitHub Actions, GitLab,
  Jenkins). 1+ contribution upstream to Spring AI / `mcp-security`.
- v1.0 (December 2026): live encryption-at-rest (`AiActProperties.Encryption`), JDBC-backed
  audit sink as a separate optional module, multi-tenant isolation patterns documented,
  Spring Security autoconfiguration adapter as an opt-in extra module.

## License

Apache License, Version 2.0. See [LICENSE](LICENSE).

## Maintainer

Built and maintained by [Francesco Bilotta](https://iambilotta.com)
([@iambilotta](https://github.com/iambilotta)). Contact: francesco@iambilotta.com.
