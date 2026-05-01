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
