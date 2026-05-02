# Security policy

`spring-aiact` is foundational to the Article 12 audit trail of any system that adopts it.
Vulnerabilities that allow tampering with the audit log, bypassing the HMAC chain, or exposing
the log to unauthorized callers are treated as critical.

## Reporting a vulnerability

**Do not open a public GitHub issue for a security report.** Send the report to:

- `francesco@iambilotta.com`
- (PGP key fingerprint will be published once Maven Central deployment is set up; until then,
  encrypt with the maintainer's GitHub-listed PGP key if you have one.)

Include:

- a minimal reproduction (Spring Boot config or curl call),
- the affected version (release tag or git SHA),
- the impact you observed,
- whether the vulnerability is publicly known.

## What to expect

- **Acknowledgement**: within three working days.
- **Triage and severity**: within seven working days.
- **Fix or mitigation**: within thirty days for critical severity, sixty for high, ninety for
  the rest. The maintainer is one person; coordinate disclosure timelines accordingly.
- **Public credit**: by default, reporters are credited in the changelog. State explicitly if
  you prefer to remain anonymous.

## Severity heuristic

Critical:
- Bypassing the HMAC chain such that a tampered NDJSON file verifies as valid.
- Reading the audit log without going through `AiActEndpointGuard`.
- Forging an Article 14 override event without the actor's consent.

High:
- Leaking PII into the audit metadata (the `MetadataSanitizer` is the chokepoint; bypasses
  count here).
- Breaking the chain seed continuity in normal operation (retention pruning is documented and
  out of scope).

Medium / Low:
- Build-time generator failures producing technically wrong technical files (Annex IV format).
- Performance regressions on large NDJSON files (>1 GB) that do not lose data.

## Out of scope

- Vulnerabilities in dependencies that have a vendor patch but no release in the upstream
  project: open the report against the upstream first; we will track the bump.
- Misconfigurations that the README explicitly warns against (default HMAC secret in
  production, `aiact.endpoints.allow-without-guard=true` in production).
- Issues that depend on an attacker already having root on the host. The audit log is designed
  to make tampering visible after the fact, not to survive a fully compromised kernel.

## Threat model (1-page summary)

Standard STRIDE notation. The library exists to make Article 12 audit logs **tamper-evident**,
which is a narrower property than "tamper-proof". This section names the threats explicitly so
adopters know what they are buying and what they have to add on top.

### Assets in scope

- **Integrity of past audit records** (the `record_hmac` chain).
- **Attribution of the writer** (which application instance wrote which record).
- **Confidentiality of audit access** (only authorised callers reach `/aiact/log/**`).
- **Availability of the audit append path** (a request that should be logged is not silently
  skipped).

### Threats and mitigations

| STRIDE | Threat | What we do | What we explicitly do NOT do |
|---|---|---|---|
| **S**poofing | An attacker forges new records that look valid. | HMAC chain keyed on `aiact.hmac.secret`. The starter refuses to boot in production with the placeholder default. Production deployments source the secret from a vault or from `secret-ref`. | Resist an attacker who has stolen the HMAC secret. Once the key is leaked, the chain is no longer trustworthy from the leak point onward; rotation is documented in `docs/PRODUCTION.md` as a controlled procedure (export + archive + re-seed). |
| **T**ampering | An attacker edits or reorders records on disk. | `record_hmac` is computed over the canonical record + `prev_hmac`. Any modification breaks `GET /aiact/log/verify` at the modified record and at every record that links back to it. Tests pin the property (`AiActLoggingAspectTest`, `RetentionPolicyServiceTest`). | Undo a deletion of the entire NDJSON file. The starter exposes `GET /aiact/log/head` so an external canary can detect a `head_hmac` that moved backward, but the canary is the adopter's responsibility. |
| **R**epudiation | The provider of the high-risk system claims a record was not written by them. | Every record carries `system_id`, `system_version`, `model_id` and `record_hmac`. Combined with the HMAC chain, a verifying party with the secret can prove the record came from the writer at that point in the chain. | Provide third-party non-repudiation. HMAC is symmetric: an external auditor with the same key can both verify and forge. Notified-body workflows that require non-repudiation will need a future asymmetric-signing mode (see ADR-0004 alternatives). |
| **I**nformation disclosure | An unauthorised caller reads the audit log via `/aiact/log/export`. | All `/aiact/log/**` endpoints route through `AiActEndpointGuard`. Default implementation deny-all with reason `no-guard-configured` in production. Adopters wire Spring Security or OPA to the `AiActEndpointGuard` SPI; the README and `docs/PRODUCTION.md` show the minimal Spring Security recipe. The starter refuses to boot in production with `allow-without-guard=true`. | Encrypt the NDJSON file at rest. `AiActProperties.Encryption` is a placeholder. Until v1.2 adopters rely on filesystem encryption (LUKS, EBS-encrypted, EFS-at-rest). Documented in the README operational notes. |
| **D**enial of service | The audit append path is overloaded and silently drops events. | `single-writer-lock` forces serialisation of writes per `system_id` so two pods do not corrupt the file. Each append is bounded by one fsync. The starter exposes `actuator/health/aiact` and the `aiact.audit.appended` / `dropped` Micrometer counters so an adopter can alert. | Buffer-and-replay during NFS outages. If the filesystem is unavailable, the append throws and the calling business method propagates the failure. The library does not silently swallow audit failures. |
| **E**levation of privilege | An attacker submits an Article 14 override that is recorded as if a HR specialist did it. | `POST /aiact/oversight/{eventId}/override` routes through the `SUBMIT_OVERRIDE` `AiActEndpointGuard` action and records the actor declared by the guard. Adopters wire the guard to their authentication. | Verify the actor's identity against an external IdP. The library trusts whatever the `AiActEndpointGuard` returns; binding it to an OAuth2 / OIDC subject is the adopter's responsibility. |

### Trust boundaries

- The application JVM is trusted: an attacker who can run code in the same JVM as the writer
  has full access to the HMAC secret and to the audit chain. The library does not pretend
  otherwise.
- The filesystem hosting `aiact.log-dir` is trusted. Filesystem-level encryption is the
  expected mitigation for at-rest confidentiality until live encryption-at-rest ships.
- Network callers reaching `/aiact/log/**` are untrusted. They must traverse `AiActEndpointGuard`.
- The HMAC secret store (vault / k8s secret / env) is trusted. The library assumes the secret
  is loaded into the JVM's memory at startup; it does not currently support reading the secret
  on every append.

### Assumptions an auditor should verify

1. The HMAC secret is not the default placeholder in any non-development profile.
   (`AiActStartupReporter` logs it at boot; the starter fails fast otherwise.)
2. `aiact.endpoints.allow-without-guard` is `false` in production. (`AiActStartupReporter`
   logs a WARN at boot if it is `true`.)
3. The `aiact.log-dir` filesystem is encrypted at rest by the host (LUKS, EBS, EFS).
4. An external canary polls `GET /aiact/log/head` periodically and alerts on a regression.
5. HMAC secret rotation, if performed, follows the export-archive-reseed procedure documented
   in `docs/PRODUCTION.md`.
