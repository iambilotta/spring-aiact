# spring-aiact

> **EU AI Act compliance, generated from your annotations.** Annotate one Spring Boot service. Get an Article 12 audit log with a tamper-evident HMAC chain at runtime, plus an Annex IV technical file, an Article 47 Declaration of Conformity and per-dataset datasheets regenerated at every build. Apache 2.0, no SaaS sign-up, no data egress.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5%2B-green.svg)](https://spring.io/projects/spring-boot)
[![CI](https://github.com/iambilotta/spring-aiact/actions/workflows/ci.yml/badge.svg)](https://github.com/iambilotta/spring-aiact/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/iambilotta/spring-aiact.svg)](https://jitpack.io/#iambilotta/spring-aiact)

[**Quick start**](#quick-start) ·
[**Runnable sample**](spring-aiact-sample) ·
[**Docker demo**](examples/docker-compose) ·
[**Production guide**](docs/PRODUCTION.md) ·
[**Changelog**](CHANGELOG.md)

---

## Without spring-aiact vs with it

You ship a Spring Boot service that does anything in **Annex III** (CV scoring, credit, biometrics, education, justice). The EU AI Act requires you, before **2 August 2026**, to keep a tamper-evident event log (Art. 12), a technical file (Art. 11 + Annex IV), a Declaration of Conformity (Art. 47), per-dataset datasheets (Art. 10) and documented oversight (Art. 14).

**Without this library**, the typical path is:

- a SaaS governance dashboard (~25k EUR/year, audit data uploaded off-prem) configured by hand and quietly drifting from the codebase, **or**
- a custom Logback channel + a Confluence page maintained by whoever last got assigned the AI Act ticket, with no tamper evidence and no link back to the running code.

**With this library**, those four artifacts come from one place: the annotations on your high-risk class. Refactor the class, the artifacts move with it. Remove an annotation and the build fails with a named gap.

```java
@Service
@AiActHighRiskSystem(id = "hiring-screener", name = "Hiring screener",
                    category = EMPLOYMENT_AND_WORKERS_MANAGEMENT, annexSubpoint = "4(a)",
                    intendedPurpose = "Score CV applicants for an engineering role.",
                    provider = "ACME")
@AiActIntendedPurpose(deploymentContext = "HR triage before human review.",
                     users = {"HR specialists"},
                     foreseeableMisuse = {"Auto-rejection without human review"})
@AiActOversight(level = HUMAN_IN_THE_LOOP, description = "Every output reviewed by HR.",
               overrideRole = "hr")
@AiActDataset(id = "cv-2025", name = "Anonymized CV corpus 2025", phase = "training",
             source = "internal-s3://cv-2025", size = "12,500", license = "internal",
             biases = {"under-representation of women in STEM"}, personalData = true)
public class HiringScreener {
    @AiActLog(modelId = "hiring-screener@0.0.1")
    public ScoringResult score(CandidateApplication application) { /* ... */ }
}
```

That single class is now the source of truth for the Article 11 dossier, the Article 12 chain, the Article 14 oversight contract and the Article 47 DoC.

## What ships, and when each piece runs

`spring-aiact` is five Maven modules. They run at three different moments and produce different artifacts. Reading the table once is the fastest way to form a mental model.

| Module | Runs at | Triggered by | Produces |
|---|---|---|---|
| `spring-aiact-core` | runtime (in your JVM) | every call to a `@AiActLog` method | one NDJSON record appended to `${aiact.log-dir}/{systemId}.ndjson`, linked to the previous record by an HMAC |
| `spring-aiact-codegen` | build (annotation processor) | `mvn compile` | nothing visible on its own, surfaces metadata for the generator |
| `spring-aiact-spring-boot-starter` | startup + runtime | `@SpringBootApplication` | wires the AOP advisor, the `/aiact/**` REST endpoints, the actuator health, the retention sweeper |
| `spring-aiact-maven-plugin` | build | `mvn verify` | `target/generated-docs/{systemId}-technical-file.md`, `-doc.pdf`, `-datasheets/*.md`. Fails the build if a companion annotation is missing on a high-risk class |
| `spring-aiact-sample` | reference | `mvn -pl spring-aiact-sample spring-boot:run` | a working `HiringScreener` you can `curl` end-to-end, see [`spring-aiact-sample`](spring-aiact-sample) |

The annotations are the contract between build time and runtime. Both halves read the same source of truth, so the dossier you submit and the log the auditor inspects can never disagree about what your system claims to be.

## What you get, by example

After one HTTP call to a method annotated `@AiActLog`, the audit log gains one record:

```json
{"event_id":"5f5f0ad6-c41b-4d08-8dd7-d7d40c8f1e23","event_kind":"INVOCATION",
 "timestamp":"2026-04-29T21:10:32.987Z","system_id":"hiring-screener",
 "system_version":"0.0.1","operation":"HiringScreener.score",
 "model_id":"hiring-screener@0.0.1","input_hash":"sha256:0a7c4d7c1c5e...",
 "output_hash":"sha256:f3e5b91a9b0e...","hash_algorithm":"SHA-256","latency_ms":3,
 "prev_hmac":"00000000000000000000000000000000",
 "record_hmac":"3e7c18a9b73f4cdbb5e91f2c83a7b4e1..."}
```

After one `mvn verify`, the technical file appears at `target/generated-docs/hiring-screener-technical-file.md`:

```markdown
# Technical File, AI Act Annex IV

**System:** Hiring screener
**System id:** `hiring-screener`
**Provider:** ACME
**Version:** 0.0.1
**Annex III category:** Annex III.4 (EMPLOYMENT_AND_WORKERS_MANAGEMENT), sub-point 4(a)
**Generated at:** 2026-04-29T21:12:26Z

## 1. General description

**Intended purpose (summary).** Score CV applicants for an engineering role.
**Deployment context.** HR triage before any human review.

**Intended user categories:**
- HR specialists

**Foreseeable misuse:**
- Auto-rejection without human review
- Use outside the engineering role context
[...nine sections total, populated from the @AiAct* annotations...]
```

The chain verifier confirms the log is intact:

```bash
$ curl -u audit:audit-pass http://localhost:8080/aiact/log/verify?system=hiring-screener
{"system_id":"hiring-screener","inspected":3,"invalid":0,"failed_event_ids":[]}
```

A working end-to-end example you can clone and run is in [`spring-aiact-sample`](spring-aiact-sample). A Docker Compose variant with a real reverse proxy is in [`examples/docker-compose`](examples/docker-compose).

## Why a tamper-evident HMAC chain matters in practice

Article 12 requires logs that detect tampering, not just logs that exist. Every record carries the HMAC of the previous record (`prev_hmac`) plus its own (`record_hmac`), both keyed with a secret only the writer knows.

Concretely: if anyone edits a single byte of an old record on disk, recalculation breaks at that point and at every record after. `GET /aiact/log/verify` walks the file and returns the exact `event_id` list that no longer matches. There is no way to remove a record silently either: a deletion shows up as a missing `prev_hmac` link.

This is the property an assessor cares about. The library does the bookkeeping.

## Why annotations and not (SaaS / DIY)

| Aspect | spring-aiact | SaaS governance vendors | DIY (Logback + custom JSON + Confluence) |
|---|---|---|---|
| Cost | Apache 2.0 free | ~25k EUR/year | Engineering time, ongoing |
| Source of truth | Code annotations | Web UI configuration | Drifts (code vs Confluence) |
| Data egress | None, on-prem | Audit data uploaded | None |
| Refactoring safety | Removing annotation removes the claim | Out of band, drifts silently | None; tribal knowledge |
| Tamper evidence | HMAC chain, deletion-detecting | Vendor-specific | Usually absent |
| Annex IV technical file | Generated, 9 sections, MD | Vendor templates | Hand-written |
| Article 47 DoC | Generated PDF | Vendor templates | Hand-written |
| Vendor lock-in | None | High | None |
| Engineering hours to onboard | Hours | Days of vendor onboarding | Weeks per system |

The trade-off: `spring-aiact` does not give you a dashboard or a sales-to-CISO conduit. It gives you the artifacts a notified body assessor wants and a git-versioned source of truth. The DIY option works for one system; the annotation approach pays back its setup time on the second system.

## Quick start

> Goal: get the audit log writing on your laptop in about ten minutes. Production-grade auth wiring is the next section.

### 1. Add the starter

`spring-aiact` is distributed via [JitPack](https://jitpack.io). JitPack builds the tag on first request and caches the artifacts; no Sonatype account needed on your side.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.iambilotta.spring-aiact</groupId>
    <artifactId>spring-aiact-spring-boot-starter</artifactId>
    <version>v1.1.0</version>
</dependency>
```

Gradle:

```kotlin
repositories { maven { url = uri("https://jitpack.io") } }
dependencies {
    implementation("com.github.iambilotta.spring-aiact:spring-aiact-spring-boot-starter:v1.1.0")
}
```

### 2. Annotate your high-risk class

See the example in [Without vs with](#without-spring-aiact-vs-with-it) above. The Maven plugin will fail your build if any of the four companions (`@AiActIntendedPurpose`, `@AiActOversight`, `@AiActDataset`, plus `@AiActHighRiskSystem` itself) is missing.

### 3. Configure

```yaml
spring:
  profiles:
    active: dev                      # tolerates the default HMAC secret on this profile

aiact:
  hmac:
    secret: ${AIACT_HMAC_SECRET}     # generate with: openssl rand -hex 32
  log-dir: /var/log/aiact
  retention: P10Y
  audit:
    single-writer-lock: true         # required when more than one pod writes
  endpoints:
    enabled: true
    base-path: /aiact
    allow-without-guard: true        # FIRST RUN ONLY. Production wires a real guard, see below.
```

### 4. Wire the Maven plugin

```xml
<pluginRepositories>
    <pluginRepository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </pluginRepository>
</pluginRepositories>

<build>
    <plugins>
        <plugin>
            <groupId>com.github.iambilotta.spring-aiact</groupId>
            <artifactId>spring-aiact-maven-plugin</artifactId>
            <version>v1.1.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>verify</goal>     <!-- fails build on missing annotation -->
                        <goal>generate</goal>   <!-- emits target/generated-docs/* -->
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 5. Run

```bash
mvn verify              # generates the technical file + DoC + datasheets
mvn spring-boot:run     # boots the app, AOP advisor wires the audit log
```

The first invocation of any `@AiActLog` method appends a record to `${aiact.log-dir}/{systemId}.ndjson`. Verify the chain at `GET /aiact/log/verify?system={systemId}` (returns `invalid: 0`).

## Production auth wiring

The first-run config above sets `allow-without-guard: true`, which lets any local caller hit `/aiact/**`. Production must register an `AiActEndpointGuard` bean. Minimal Spring Security wiring:

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
        boolean canWrite = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("AIACT_WRITE"));
        return switch (action) {
            case EXPORT_LOG, VERIFY_LOG, READ_HEAD ->
                canRead ? AiActEndpointGuard.Decision.allow()
                        : AiActEndpointGuard.Decision.deny("requires AIACT_READ");
            case SUBMIT_OVERRIDE ->
                canWrite ? AiActEndpointGuard.Decision.allow()
                         : AiActEndpointGuard.Decision.deny("requires AIACT_WRITE");
        };
    };
}
```

```yaml
spring:
  profiles:
    active: prod
aiact:
  endpoints:
    allow-without-guard: false       # production default
```

The starter does not depend on Spring Security; OPA, API key checks, mTLS subject mappings, or any other auth source plug into the same SPI. OPA + multi-tenant examples in [`docs/PRODUCTION.md`](docs/PRODUCTION.md).

## Articles covered (v1.0)

| Article | What spring-aiact does |
|---|---|
| Article 10 | `@AiActDataset` declarations and per-dataset Markdown datasheets |
| Article 11 + Annex IV | Build-time Markdown technical file generator (nine sections) |
| Article 12 | NDJSON append-only audit log with HMAC chain. Tamper detection via `/aiact/log/verify` |
| Article 13 | `@AiActIntendedPurpose` populates the Instructions for Use section |
| Article 14 | `@AiActOversight` declarations and a REST oversight endpoint that records overrides as second events linked to the original |
| Article 15 | `@AiActAccuracyMetric` declarations forwarded to the technical file |
| Article 47 | Declaration of Conformity PDF with signature placeholder |
| Annex VII | `AuditExportPackager` produces a signed ZIP for notified body submission |

## REST endpoints

| Method | Path | Purpose | Guard action |
|---|---|---|---|
| GET | `/aiact/log/export?system=&from=&to=` | Stream NDJSON slice | `EXPORT_LOG` |
| GET | `/aiact/log/verify?system=&from=&to=` | Chain verification report | `VERIFY_LOG` |
| GET | `/aiact/log/head?system=` | Current chain head HMAC (tamper canary) | `READ_HEAD` |
| POST | `/aiact/oversight/{eventId}/override` | Record an Article 14 override | `SUBMIT_OVERRIDE` |
| GET | `/actuator/health/aiact` | Up/down + log dir, retention, multi-process status | none (Actuator) |

Every call passes through the configured `AiActEndpointGuard`. The default deny-all guard returns `403` with the reason `no-guard-configured`.

## Operational notes the README will not let you skip

**Multi-pod deployment.** The audit log is one append-only NDJSON file per system id. With more than one pod writing to the same file (Kubernetes ReadWriteMany volume, two JVMs on the same host), keep `aiact.audit.single-writer-lock=true` (default). Each append acquires an OS file lock, tails the file under the lock and recomputes the chain head from disk. On NFSv3 without lockd, `flock` is not reliable; prefer NFSv4 or a single-writer deployment.

**Retention prunes the chain seed.** When `RetentionPolicyService` removes records older than the configured horizon, the kept slice carries its original `prev_hmac` (the HMAC of the now-deleted predecessor). A verifier walking from `CHAIN_SEED` will see one mismatch on that boundary record. Documented in `RetentionPolicyServiceTest`. Export before prune if you need pre-cutoff verifiability.

**Encryption at rest is a placeholder, planned for a future minor.** `AiActProperties.Encryption` is reserved for the implementation and currently does nothing. Until that lands, rely on filesystem encryption (LUKS, EFS-at-rest, EBS-encrypted). Do not interpret `aiact.encryption.enabled=true` as live encryption.

**HMAC secret default in production.** The starter refuses to boot if `aiact.hmac.secret` is the placeholder `change-me-please` and no development profile is active. Set `aiact.hmac.fail-on-default-in-prod=false` only as an emergency hotfix.

## FAQ

**Does it slow down my endpoints?** The advisor on `@AiActLog` adds one Jackson serialization plus one SHA-256 of the input and the output. Both are CPU-bound and stay off the I/O path. The audit append is buffered and runs through a single-writer file lock, so the request thread only waits on it when you opt into `aiact.audit.flush-mode=sync`. If your handler already touches a database, the AI Act work falls inside the same I/O bucket and is rarely the bottleneck. Measure with your own workload before committing.

**Does it work without Spring Security?** Yes. The auth surface is one SPI (`AiActEndpointGuard`); plug in OPA, API keys, mTLS subjects, anything. A Spring Security example is in `docs/PRODUCTION.md`.

**Can I store the audit log in Postgres / S3?** Not in v1.0. `AuditLogService` is an interface; a JDBC sink is planned as a separate optional module in a future minor. Until then, register your own implementation if you must.

**My system is not in Annex III. Should I still use this?** No. The starter is for high-risk systems. If your system is general-purpose AI under Article 51 or limited-risk under Article 50, you have different obligations and this is not the right tool.

**Will my audit log work after a HMAC key rotation?** No, by design. Rotation is documented in `docs/PRODUCTION.md` as a controlled procedure (export old chain, archive, re-seed). An attacker who steals the key loses access to forge new records but the old chain remains verifiable with the old key, which is what an auditor will ask for.

**Does it support Spring Boot 2.x?** No. v1.0 targets Spring Boot 3.5+ and Java 21+.

## Out of scope

- Not a wrapper around Logback. Without the Annex IV generator, this would be an audit logger, not an AI Act compliance tool.
- Not a custom Annex III. The enum reflects the eight official Annex III points; when the regulation is amended, the enum is amended.
- Not a certification. Only a notified body certifies; the library produces the evidence pack you submit to one.
- Not a replacement for governance. The annotations are a delivery vehicle for decisions your data governance and risk management process already made. Empty annotations produce visible gap markers, not hallucinated text.

## Roadmap

- **v1.0 (current, May 2026):** API freeze. Apache 2.0, JitPack distribution, single SPI for auth (`AiActEndpointGuard`), file-locked NDJSON audit log with HMAC chain, deny-by-default endpoints, fail-fast on the default HMAC secret in production, configuration metadata, actuator health indicator. Internal `ObjectMapper` is no longer a Spring bean (no shadowing of the application's primary mapper).
- **Planned for a future minor:** CI templates (GitHub Actions, GitLab, Jenkins) wrapping `verify`. JDBC-backed audit sink as a separate optional module. Multi-tenant isolation patterns documented. Spring Security autoconfiguration adapter as an opt-in extra module. Live encryption-at-rest (`AiActProperties.Encryption`). Maven Central namespace `com.iambilotta.spring` published in addition to JitPack.

## About

Built by [Francesco Bilotta](https://iambilotta.com), Lead Software Engineer. The library is the externalised version of patterns I have wired into Spring Boot products in regulated environments (real estate, fintech-adjacent), where the same problem (tamper-evident audit, regenerable dossier from code) kept getting solved in private repos. `spring-aiact` is the Apache 2.0 distillation: same patterns, scoped to AI Act high-risk systems, made public so they stop being rebuilt from scratch on every project.

Sister repo [spring-gdpr](https://github.com/iambilotta/spring-gdpr) covers GDPR (Article 30 ROPA, Article 35 DPIA, Article 17 erasure, Article 5 retention) on the same evidence-as-code foundation. The two compose: a service that scores CVs typically falls under both regulations.

Contact: francesco@iambilotta.com. Security reports: see [SECURITY.md](SECURITY.md).

## License

Apache License, Version 2.0. See [LICENSE](LICENSE).
