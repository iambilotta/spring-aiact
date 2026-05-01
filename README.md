# spring-aiact

> **EU AI Act compliance-by-annotation for Spring Boot.** Annotate your high-risk AI system,
> get an Article 12 audit log with tamper-evident HMAC chain and an Annex IV technical file
> regenerated from the code on every CI run. Apache 2.0, no SaaS sign-up, no data egress.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5%2B-green.svg)](https://spring.io/projects/spring-boot)
[![CI](https://github.com/iambilotta/spring-aiact/actions/workflows/ci.yml/badge.svg)](https://github.com/iambilotta/spring-aiact/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/badge/maven--central-not%20yet%20published-lightgrey.svg)](https://central.sonatype.com/)
[![Status](https://img.shields.io/badge/status-alpha%20(v0.1)-yellow.svg)](#project-status)

---

## TL;DR

You ship a Spring Boot service that does anything in Annex III (CV scoring, credit, biometrics,
education, justice). The EU AI Act requires you to keep a tamper-evident event log, a technical
file, a declaration of conformity and documented oversight. `spring-aiact` produces all four
from annotations on your code. Three steps to get going:

1. add the starter,
2. annotate your high-risk class with `@AiActHighRiskSystem` + four companions,
3. configure an HMAC secret and an `AiActEndpointGuard` bean. Done.

The deadline for high-risk obligations is **2 August 2026**. This project ships before that.

## Table of contents

- [How it works in 30 seconds](#how-it-works-in-30-seconds)
- [Why annotations and not a SaaS GRC tool](#why-annotations-and-not-a-saas-grc-tool)
- [Quick start (15 minutes)](#quick-start-15-minutes)
- [60-second runnable demo](#60-second-runnable-demo)
- [Articles covered](#articles-covered-v01)
- [REST endpoints](#rest-endpoints)
- [Operational notes the README will not let you skip](#operational-notes-the-readme-will-not-let-you-skip)
- [FAQ](#faq)
- [Anti-patterns explicitly out of scope](#anti-patterns-explicitly-out-of-scope)
- [Project status](#project-status)
- [Roadmap](#roadmap)

## How it works in 30 seconds

```
   Your code                     spring-aiact                  Output artifacts
                                                          
  @AiActHighRiskSystem      AOP advisor (runtime)         /var/log/aiact/sys.ndjson
  @AiActIntendedPurpose  ->                            ->   tamper-evident HMAC chain
  @AiActOversight              build-time generator    ->   target/generated-docs/
  @AiActDataset                                                technical-file.md
  @AiActAccuracyMetric         REST endpoints          ->   /aiact/log/{export,verify,head}
  @AiActLog                    actuator health         ->   /actuator/health/aiact
                               retention sweeper       ->   per-system NDJSON pruning
```

The annotations are the source of truth. The audit log, the Annex IV technical file, the DoC
PDF and the dataset datasheets are build artifacts regenerated whenever the code changes.

## Why annotations and not a SaaS GRC tool

| Aspect | spring-aiact | Sprinto / Centraleyes / Credo AI |
|---|---|---|
| Cost | Apache 2.0 free | ~25k EUR/year SaaS subscriptions |
| Source of truth | Code annotations | Web UI configuration |
| Data egress | None, on-prem | Required (audit data uploaded) |
| Refactoring safety | Removing annotation removes the claim, code review catches it | Out of band, drifts silently |
| Compliance update cadence | Same as the code | Same as the vendor's release cycle |
| Vendor lock-in | None, plain Markdown / NDJSON / PDF | Vendor file formats |
| Replaces a notified body | No (anti-overclaim by design) | Sometimes implied |

The trade-off: spring-aiact does not give you a dashboard, a vendor relationship to point at,
or a sales-to-CISO conduit. It gives you the artifacts a notified body assessor wants and a
git-versioned source of truth. Pick accordingly.

## Quick start (15 minutes)

### 1. Add the starter (1 minute)

```xml
<dependency>
    <groupId>com.iambilotta.spring</groupId>
    <artifactId>spring-aiact-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. Annotate your high-risk class (5 minutes)

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
        // your real scoring logic
    }
}
```

The Maven plugin will fail your build if any of the four companion annotations
(`@AiActIntendedPurpose`, `@AiActOversight`, `@AiActDataset`, plus `@AiActHighRiskSystem`
itself) is missing.

### 3. Configure (5 minutes)

```yaml
aiact:
  hmac:
    secret: ${AIACT_HMAC_SECRET}     # required, generate with: openssl rand -hex 32
  log-dir: /var/log/aiact
  retention: P10Y
  audit:
    single-writer-lock: true         # default; required when more than one pod writes
  endpoints:
    enabled: true
    base-path: /aiact
    allow-without-guard: false       # default; production must register a real guard
```

### 4. Wire the auth guard (3 minutes)

The starter's default guard refuses every `/aiact/**` call. Register a real one:

```java
@Bean
AiActEndpointGuard aiActEndpointGuard() {
    return (systemId, action) -> {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return AiActEndpointGuard.Decision.deny("not-authenticated");
        }
        boolean canRead = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("AIACT_READ"));
        return switch (action) {
            case EXPORT_LOG, VERIFY_LOG, READ_HEAD ->
                canRead ? AiActEndpointGuard.Decision.allow()
                        : AiActEndpointGuard.Decision.deny("requires AIACT_READ");
            case SUBMIT_OVERRIDE -> /* check AIACT_WRITE authority */ ...;
        };
    };
}
```

The starter does not depend on Spring Security; OPA, API key checks, mTLS subject mappings or
any other auth source plug into the same SPI. Full examples in
[docs/PRODUCTION.md](docs/PRODUCTION.md).

### 5. Wire the Maven plugin (1 minute)

```xml
<plugin>
    <groupId>com.iambilotta.spring</groupId>
    <artifactId>spring-aiact-maven-plugin</artifactId>
    <version>0.1.0</version>
    <executions>
        <execution>
            <goals>
                <goal>verify</goal>     <!-- fails build on missing annotation -->
                <goal>generate</goal>   <!-- emits target/generated-docs/* -->
            </goals>
        </execution>
    </executions>
</plugin>
```

### 6. What you get

After the next `mvn verify`:

- `target/generated-docs/{systemId}-technical-file.md` (Annex IV, 9 sections)
- `target/generated-docs/{systemId}-doc.pdf` (Article 47 DoC, signature placeholder)
- `target/generated-docs/{systemId}-dataset-{id}.md` (one per `@AiActDataset`)

After the first runtime invocation:

- `${aiact.log-dir}/{systemId}.ndjson` (one Article 12 record per `@AiActLog` call)
- `GET /aiact/log/verify?system={systemId}` returns `invalid: 0`

## 60-second runnable demo

```bash
git clone https://github.com/iambilotta/spring-aiact
cd spring-aiact/examples/docker-compose
docker compose up --build
```

In another terminal:

```bash
# Score a candidate
curl -X POST http://localhost:8080/hiring/score \
  -H 'Content-Type: application/json' \
  -d '{"candidateId":"c-1","cvText":"hello"}'

# Read the audit log (auth: audit / audit-pass)
curl -u audit:audit-pass \
  'http://localhost:8080/aiact/log/export?system=hiring-screener'

# Verify the chain (should report invalid: 0)
curl -u audit:audit-pass \
  'http://localhost:8080/aiact/log/verify?system=hiring-screener'
```

See [`examples/docker-compose/README.md`](examples/docker-compose/README.md) for the tamper
test that walks through editing the NDJSON on disk and watching the verify endpoint flag the
mismatch.

## Articles covered (v0.1)

| Article | What spring-aiact does |
|---|---|
| Article 10 | `@AiActDataset` declarations and per-dataset Markdown datasheets. |
| Article 11 + Annex IV | Build-time Markdown technical file generator (nine sections). |
| Article 12 | NDJSON append-only audit log with HMAC chain. Tamper detection via `/aiact/log/verify`. |
| Article 13 | `@AiActIntendedPurpose` populates the Instructions for Use section. |
| Article 14 | `@AiActOversight` declarations and REST oversight endpoint that records overrides as second events linked to the original. |
| Article 15 | `@AiActAccuracyMetric` declarations forwarded to the technical file. |
| Article 47 | Declaration of Conformity PDF with signature placeholder. |
| Annex VII | `AuditExportPackager` produces a signed ZIP for notified body submission. |

## REST endpoints

| Method | Path | Purpose | Guard action |
|---|---|---|---|
| GET | `/aiact/log/export?system=&from=&to=` | Stream NDJSON slice | `EXPORT_LOG` |
| GET | `/aiact/log/verify?system=&from=&to=` | Chain verification report | `VERIFY_LOG` |
| GET | `/aiact/log/head?system=` | Current chain head HMAC (tamper canary) | `READ_HEAD` |
| POST | `/aiact/oversight/{eventId}/override` | Record an Article 14 override | `SUBMIT_OVERRIDE` |
| GET | `/actuator/health/aiact` | Up/down + log dir, retention, multi-process status | none (Actuator) |

Every call passes through the configured `AiActEndpointGuard`. The default deny-all guard
returns `403` with the reason `no-guard-configured`.

## Operational notes the README will not let you skip

**Multi-pod deployment.** The audit log is one append-only NDJSON file per system id. With more
than one pod writing to the same file (Kubernetes ReadWriteMany volume, two JVMs on the same
host, etc), keep `aiact.audit.single-writer-lock=true` (default). Each append acquires an OS
file lock, tails the file under the lock and recomputes the chain head from disk. Cost: one
fsync per append (~1-3 ms on local SSD, slower on networked FS). On NFSv3 without lockd,
`flock` is not reliable; prefer NFSv4 or a single-writer deployment.

**Retention prunes the chain seed.** When `RetentionPolicyService` removes records older than
the configured horizon, the kept slice carries its original `prev_hmac` (the HMAC of the
now-deleted predecessor). A verifier walking from `CHAIN_SEED` will see one mismatch on that
boundary record. This is documented in `RetentionPolicyServiceTest`; export before prune if
you need pre-cutoff verifiability.

**Encryption at rest is a v1.0 placeholder.** `AiActProperties.Encryption` is reserved for the
future implementation and currently does nothing. Until v1.0 lands, rely on filesystem
encryption (LUKS, EFS-at-rest, EBS-encrypted). Do not interpret `aiact.encryption.enabled=true`
as live encryption.

**HMAC secret default in production.** The starter refuses to boot if `aiact.hmac.secret` is
the placeholder `change-me-please` and no development profile is active. Set
`aiact.hmac.fail-on-default-in-prod=false` only as an emergency hotfix.

## FAQ

**Is spring-aiact a certification?** No. Only a notified body certifies. spring-aiact produces
the evidence pack you submit to one.

**Does it work without Spring Security?** Yes. The auth surface is one SPI
(`AiActEndpointGuard`); plug in OPA, API keys, mTLS subjects, anything. A Spring Security
example is in `docs/PRODUCTION.md`.

**Will it slow down my endpoints?** The AOP advisor adds one Jackson serialization + SHA-256
hash per call (sub-millisecond on typical payloads). The audit append adds one fsync (~1-3 ms
on local SSD) when `single-writer-lock=true`. If your endpoints already touch a database,
spring-aiact is in the noise.

**Can I store the audit log in Postgres / S3?** Not in v0.1. `AuditLogService` is an interface;
a JDBC sink lands in v1.0 as a separate optional module. Until then, register your own
implementation if you must.

**My system is not in Annex III. Should I still use this?** No. The starter is for high-risk
systems. If your system is general-purpose AI under Article 51 or limited-risk under Article
50, you have different obligations and this is not the right tool.

**Will my audit log work after a HMAC key rotation?** No, by design. Rotation is documented in
`docs/PRODUCTION.md` as a controlled procedure (export old chain, archive, re-seed). An
attacker who steals the key loses access to forge new records but the old chain remains
verifiable with the old key, which is what an auditor will ask for.

**Does it support Spring Boot 2.x?** No. v0.1 targets Spring Boot 3.5+ and Java 21+. The Spring
ecosystem has moved on; backporting would mean a fork, not a configuration toggle.

## Anti-patterns explicitly out of scope

- **Not a wrapper around Logback.** Without the Annex IV generator, this would be an audit
  logger, not an AI Act compliance tool.
- **No custom Annex III categories.** The enum reflects the eight official Annex III points.
  When the regulation is amended, the enum is amended; never invent values.
- **No certification claim.** Only a notified body certifies. spring-aiact produces the
  evidence pack you submit to one.
- **Not a SaaS replacement for governance.** The annotations are a delivery vehicle for
  decisions your data governance and risk management process already made. Empty annotations
  produce visible gap markers, not hallucinated text.

## Project status

Alpha (v0.1). The surface is stable, the tests are deliberate, the production deployment guide
is real. Maven Central publication and a production case study land before v0.5 (July 2026).
Expect breaking changes between alpha and v1.0; review the `CHANGELOG.md` on every bump.

## Roadmap

- **v0.1 (June 2026, target):** the surface above. Apache 2.0, Maven Central. Single SPI for
  auth (`AiActEndpointGuard`), file-locked audit log, deny-by-default endpoints, fail-fast on
  the default HMAC secret in production, configuration metadata, actuator health indicator.
- **v0.5 (July 2026):** build-time `verify` integrated with CI templates (GitHub Actions,
  GitLab, Jenkins). One contribution upstream to Spring AI / `mcp-security`. Production case
  study from a real adopter.
- **v1.0 (December 2026):** live encryption-at-rest (`AiActProperties.Encryption`), JDBC-backed
  audit sink as a separate optional module, multi-tenant isolation patterns documented, Spring
  Security autoconfiguration adapter as an opt-in extra module.

## License

Apache License, Version 2.0. See [LICENSE](LICENSE).

## Maintainer

Built and maintained by [Francesco Bilotta](https://iambilotta.com)
([@iambilotta](https://github.com/iambilotta)). Contact: francesco@iambilotta.com.
Security reports: see [SECURITY.md](SECURITY.md).
