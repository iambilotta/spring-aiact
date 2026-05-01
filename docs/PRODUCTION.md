# Production deployment guide

This guide is the answer to the question "I cloned `spring-aiact`, my CI is green, what do I
need to do before pointing it at a real high-risk system in production?". It is not a marketing
document. Every section names a concrete failure mode the default does not protect against and
the configuration that closes it.

Target reader: engineer doing the EU AI Act conformity assessment for one Spring Boot service.
Time to follow this guide end-to-end on a fresh deployment: ~60 minutes.

## 1. Configure the HMAC secret outside the source tree

The default value of `aiact.hmac.secret` is `change-me-please`. The starter refuses to boot if
this value is detected and no development profile is active.

Recommended setup (Spring Cloud Config / HashiCorp Vault):

```yaml
spring:
  config:
    import: "vault://"
  cloud:
    vault:
      uri: https://vault.internal:8200
      token: ${VAULT_TOKEN}
      kv:
        backend: secret
        application-name: spring-aiact

aiact:
  hmac:
    secret-ref: ${vault.spring-aiact.hmac.secret}
```

Plain environment variable also works:

```yaml
aiact:
  hmac:
    secret: ${AIACT_HMAC_SECRET}
```

Generate the secret with `openssl rand -hex 32`. Treat it like a master key: rotation is
disruptive (see section 4).

## 2. Wire `AiActEndpointGuard` to your auth stack

The default guard refuses every call. The minimal Spring Security wiring:

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

Tighter, multi-tenant variant: derive the allowed `systemId` set from the principal and reject
when `systemId` is not in that set. The library hands the system id to the guard precisely so
this can be enforced without forking the controllers.

OPA / Open Policy Agent example (sketch):

```java
@Bean
AiActEndpointGuard aiActEndpointGuard(OpaClient opa) {
    return (systemId, action) -> {
        var input = Map.of(
            "subject", currentSubject(),
            "system_id", systemId,
            "action", action.name()
        );
        return opa.evaluate("data.aiact.allow", input)
            ? AiActEndpointGuard.Decision.allow()
            : AiActEndpointGuard.Decision.deny("opa-denied");
    };
}
```

Whatever you pick, write a test that asserts the deny path returns 403, not 200. The deploy
that worked once because the dev profile was on by accident is the deploy that ends up audited.

## 3. Multi-pod and shared filesystem caveats

`spring-aiact` writes one append-only NDJSON file per system id. With more than one writer the
default protects you, but only if the underlying filesystem honors the OS file lock.

| Environment | Status | Notes |
|---|---|---|
| Single pod, local SSD | Safe with `single-writer-lock=true` (default) | The OS lock and the in-process lock combine to serialize writes. |
| Multiple pods, AWS EBS / GCE PD (single-writer volume) | Not supported | The volume cannot be attached to two pods. Use one pod (Deployment with replicas=1, RWO PVC). |
| Multiple pods, EFS / Filestore / Azure Files | Safe | NFSv4.1 honors `flock`. Test with the recipe at the bottom of this section. |
| Multiple pods, NFSv3 without lockd | **Unsafe** | Skip; route writers to one pod via leader election or use a single-writer Deployment. |
| Multiple pods, Longhorn / Ceph CephFS | Safe | Verified with `flock` on a shared volume. |
| Multiple pods, hostPath | Unsafe | Two pods on the same node may not see consistent locks across kernel namespaces. |

Recipe to verify your filesystem honors `flock`:

```bash
# Pod 1
exec 9>/mnt/audit/.lock-test
flock -x 9
sleep 30
flock -u 9

# Pod 2 in parallel
exec 9>/mnt/audit/.lock-test
flock -x -w 5 9 || echo "flock not honored, do not deploy"
```

If pod 2 succeeds within 5 seconds, the lock did not block; the filesystem is unsafe.

## 4. HMAC key rotation playbook

The HMAC chain is bound to a single key. Rotation is the only place where the chain breaks
intentionally. Plan it ahead of time.

```text
0. Stop write traffic to the affected systems (drain pod, flip a feature flag, whatever).
1. Export the current chain to long-term storage:
     curl -X GET 'https://app/aiact/log/export?system=hiring-screener' \
       -H 'Authorization: Bearer <token-with-AIACT_READ>' \
       > exports/hiring-screener-$(date +%Y%m%d).ndjson
2. Move the on-disk NDJSON aside:
     mv aiact-logs/hiring-screener.ndjson archives/hiring-screener-pre-$(date +%Y%m%d).ndjson
3. Push the new HMAC secret into Vault / KMS / env.
4. Restart the application. The chain reseeds from CHAIN_SEED on the empty file.
5. Start write traffic.
6. Archive the previous chain export together with a short note recording:
     - the rotation date,
     - the new chain head HMAC after the first record,
     - the actor who performed the rotation.
```

The export at step 1 is verifiable by anyone holding the previous key. After rotation, only
the new key verifies the new chain. Auditors will ask for both keys and both archives.

## 5. Retention export workflow

Pruning preserves the chain HMAC fields on the kept slice but breaks the chain seed continuity
(documented in `RetentionPolicyServiceTest`). To preserve full pre-cutoff verifiability:

```text
1. Disable the daily retention sweeper for the system in question:
     aiact.retention-sweeper.enabled=false
2. Run the export:
     curl -X GET 'https://app/aiact/log/export?system=hiring-screener&from=2016-01-01&to=2017-12-31' \
       -H 'Authorization: Bearer <token>' \
       > exports/hiring-screener-2016-2017.ndjson
3. Sign and archive the export:
     openssl dgst -sha256 -hmac "$AIACT_HMAC_SECRET" \
       exports/hiring-screener-2016-2017.ndjson \
       > exports/hiring-screener-2016-2017.ndjson.hmac
4. Re-enable the sweeper. The next sweep will prune the records older than the retention
   horizon.
```

The `AuditExportPackager` produces the same shape (manifest + HMAC) for ad-hoc submission to a
notified body. Use it when the export is a one-off rather than part of the rotation cycle.

## 6. Observability without leaking the audit log

Three signals are worth surfacing to your existing metrics stack. Do not feed the audit log
itself to a SIEM that is broader than your audit perimeter; the whole point of evidence-as-code
is that the dossier is reproducible from the file.

- **Append rate per system** (counter): expose by wrapping `AuditLogService` with your own
  Micrometer-backed decorator. A flat-line on a system that should be active is a deployment
  smoke alarm, an audit anomaly, or both.
- **Chain head HMAC** (gauge / poll): the `/aiact/log/head?system=X` endpoint returns the
  current head. A monitoring system polling once a minute can detect head changes that do not
  correspond to an application metric increase, which is the cheap tamper canary.
- **Verify run** (gauge / cron): once a day, run `verify` for each system over the last 24
  hours and emit `inspected` and `invalid`. Page on `invalid > 0`.

## 7. CI gate via the Maven plugin

```xml
<plugin>
    <groupId>com.iambilotta.spring</groupId>
    <artifactId>spring-aiact-maven-plugin</artifactId>
    <version>0.1.0</version>
    <executions>
        <execution>
            <id>aiact-verify</id>
            <goals><goal>verify</goal></goals>
            <configuration>
                <warningOnly>false</warningOnly>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Bind to `verify` (default) and let the build fail if a class is annotated `@AiActHighRiskSystem`
without `@AiActIntendedPurpose`, `@AiActOversight`, or a same-package `@AiActDataset`. The
GitHub Actions workflow in this repo runs `mvn verify -DskipTests` against the sample app on
every PR; copy that step into your own pipeline.

## 8. What you still own

The starter does not replace your data governance, your incident response process, or your
notified body relationship. It produces the artifacts; your team produces the meaning.

In particular:

- Article 10 expects you to keep a separate data governance record per dataset; the
  per-dataset datasheet generated from `@AiActDataset` is a starting point, not a substitute.
- Article 14 expects a documented oversight protocol; the override events are evidence that
  the protocol ran, not the protocol itself.
- Article 73 (incident reporting) is on you. The audit log is your raw material.

The README anti-pattern section is the short version of this. Keep it visible to anyone using
or selling the project.
