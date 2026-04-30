# spring-aiact

> **EU AI Act compliance-by-annotation for Spring Boot.** Annotate your high-risk AI system,
> get an Article 12 audit log with tamper-evident HMAC chain and an Annex IV technical file
> regenerated from the code on every CI run. Apache 2.0, no SaaS sign-up, no data egress.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5%2B-green.svg)](https://spring.io/projects/spring-boot)
[![CI](https://github.com/iambilotta/spring-aiact/actions/workflows/ci.yml/badge.svg)](https://github.com/iambilotta/spring-aiact/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/iambilotta/spring-aiact.svg)](https://jitpack.io/#iambilotta/spring-aiact)
[![Status](https://img.shields.io/badge/status-alpha%20(v0.1)-yellow.svg)](#project-status)

[**Quick start**](#quick-start-first-run-in-15-minutes) ·
[**60-second demo**](#60-second-runnable-demo) ·
[**FAQ**](#faq) ·
[**Production guide**](docs/PRODUCTION.md) ·
[**Changelog**](CHANGELOG.md) ·
[**Security policy**](SECURITY.md)

---

## TL;DR

You ship a Spring Boot service that does anything in Annex III (CV scoring, credit, biometrics,
education, justice). The EU AI Act requires you to keep a tamper-evident event log, a technical
file, a declaration of conformity and documented oversight. `spring-aiact` produces all four
from annotations on your code. Three steps to get going:

1. add the starter,
2. annotate your high-risk class with `@AiActHighRiskSystem` + four companions,
3. configure an HMAC secret. Done in 15 minutes for a first run, ~45 minutes for a
   production-ready deployment with real auth.

The deadline for high-risk obligations is **2 August 2026**. This project ships before that.

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

## Why annotations and not (SaaS / DIY logging)

| Aspect | spring-aiact | Sprinto / Centraleyes / Credo AI | DIY (Logback + custom JSON + Confluence) |
|---|---|---|---|
| Cost | Apache 2.0 free | ~25k EUR/year SaaS | Ongoing engineering time |
| Source of truth | Code annotations | Web UI configuration | Drifts (code vs Confluence) |
| Data egress | None, on-prem | Required (audit data uploaded) | None |
| Refactoring safety | Removing annotation removes the claim; code review catches it | Out of band, drifts silently | None; tribal knowledge |
| Tamper evidence | HMAC chain, deletion-detecting | Vendor-specific | Usually absent |
| Annex IV technical file | Generated, 9 sections, MD | Vendor templates | Hand-written |
| Article 47 DoC | Generated PDF with signature placeholder | Vendor templates | Hand-written |
| Vendor lock-in | None | High | None |
| Replaces a notified body | No (anti-overclaim by design) | Sometimes implied | No |
| Engineering hours to build | Hours (just annotate) | Days (vendor onboarding) | Weeks per system |

The trade-off: spring-aiact does not give you a dashboard, a vendor relationship to point at,
or a sales-to-CISO conduit. It gives you the artifacts a notified body assessor wants and a
git-versioned source of truth. The DIY option works for one system; if you have three or more
high-risk systems, the annotation approach pays back its setup time on the second adoption.

## What you actually get (sample output)

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

After one `mvn verify`, the technical file appears at
`target/generated-docs/hiring-screener-technical-file.md`:

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

And the chain verifier confirms the HMAC chain is valid:

```bash
$ curl -u audit:audit-pass http://localhost:8080/aiact/log/verify?system=hiring-screener
{"system_id":"hiring-screener","inspected":3,"invalid":0,"failed_event_ids":[]}
```

When the file is tampered with on disk, the verifier reports the exact event ids that broke
the chain. See [`examples/docker-compose/README.md`](examples/docker-compose/README.md) for a
walk-through of the tamper test.

## Quick start (first run in 15 minutes)

> Goal of this section: get the audit log writing on your laptop. Production-grade auth wiring
> is the next section after this one.

### 1. Add the starter (1 minute)

`spring-aiact` is distributed via [JitPack](https://jitpack.io) during the alpha phase. Maven
Central publication is in flight; once available, the coordinates switch to
`com.iambilotta.spring:spring-aiact-spring-boot-starter`.

Add the JitPack repository, then the starter dependency:

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
    <version>v0.1.1</version>
</dependency>
```

Gradle:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
dependencies {
    implementation("com.github.iambilotta.spring-aiact:spring-aiact-spring-boot-starter:v0.1.1")
}
```

> **Build from source.** If your environment cannot reach JitPack (corporate proxy, air-gapped
> network) clone the repo and run `./mvnw install` to publish to your local Maven repository,
> then depend on `com.iambilotta.spring:spring-aiact-spring-boot-starter:0.1.1` directly. The
> coordinate uses `0.1.1` (no `v` prefix) when built from the pom; JitPack uses the git tag
> verbatim, hence `v0.1.1`.

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

### 3. Configure (3 minutes)

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
    single-writer-lock: true         # default; required when more than one pod writes
  endpoints:
    enabled: true
    base-path: /aiact
    allow-without-guard: true        # FIRST RUN ONLY. Production wires a real guard, see below.
```

### 4. Wire the Maven plugin (1 minute)

The plugin needs the JitPack plugin repository declared in your `pom.xml`:

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
            <version>v0.1.1</version>
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

The first invocation of any `@AiActLog` method appends a record to
`${aiact.log-dir}/{systemId}.ndjson`. Verify the chain at
`GET /aiact/log/verify?system={systemId}` (returns `invalid: 0`).

## Production auth wiring (next 30 minutes)

The first-run config above sets `allow-without-guard: true`, which lets any local caller hit
`/aiact/**`. **Production must register a real `AiActEndpointGuard` bean.** Minimal Spring
Security wiring:

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

Then flip the production yaml:

```yaml
spring:
  profiles:
    active: prod

aiact:
  endpoints:
    allow-without-guard: false       # production default
```

The starter does not depend on Spring Security; OPA, API key checks, mTLS subject mappings,
or any other auth source plug into the same SPI. OPA + multi-tenant examples in
[`docs/PRODUCTION.md`](docs/PRODUCTION.md).

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

Alpha (v0.1). The surface is stable enough to evaluate, the tests are deliberate, the
production deployment guide is real. Maven Central publication and a production case study
land before v0.5 (July 2026). Expect breaking changes between alpha and v1.0; review
[`CHANGELOG.md`](CHANGELOG.md) on every bump.

This is a single-maintainer project. Bug reports and security disclosures get prioritized over
feature requests; see [`SECURITY.md`](SECURITY.md) and [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Adopters

If you are running spring-aiact in production, open a PR adding a row here. Anonymous entries
("EU mid-market HR-tech, ~250 employees, Annex III.4") are welcome and useful as social proof
without breaking anyone's legal review process.

| Organization | Industry | Annex III categories | Adopted since |
|---|---|---|---|
| _yours could be here_ | | | |

## Roadmap

- **v0.1 (May 2026, shipped):** the surface above. Apache 2.0, distributed via JitPack.
  Single SPI for auth (`AiActEndpointGuard`), file-locked audit log, deny-by-default endpoints,
  fail-fast on the default HMAC secret in production, configuration metadata, actuator health
  indicator.
- **v0.5 (July 2026):** Maven Central publication under `com.iambilotta.spring`. Build-time
  `verify` integrated with CI templates (GitHub Actions, GitLab, Jenkins). One contribution
  upstream to Spring AI / `mcp-security`. Production case study from a real adopter.
- **v1.0 (December 2026):** live encryption-at-rest (`AiActProperties.Encryption`), JDBC-backed
  audit sink as a separate optional module, multi-tenant isolation patterns documented, Spring
  Security autoconfiguration adapter as an opt-in extra module.

## License

Apache License, Version 2.0. See [LICENSE](LICENSE).

## Maintainer

Built and maintained by [Francesco Bilotta](https://iambilotta.com)
([@iambilotta](https://github.com/iambilotta)). Contact: francesco@iambilotta.com.
Security reports: see [SECURITY.md](SECURITY.md).
