# ADR-0002: NDJSON file with HMAC chain as the default Article 12 sink

- **Status:** accepted
- **Date:** 2026-04-29
- **Deciders:** Francesco Bilotta

## Context

Article 12 of the AI Act requires the provider of a high-risk system to keep automatic event logs over the system's lifetime, with sufficient detail that a notified body or a market surveillance authority can reconstruct the inferences. The shape of the log is up to the provider, but two non-negotiable properties are implied: the log is append-only, and tampering can be detected.

Two storage shapes are realistic:

- a relational database table,
- a flat file in a structured line-oriented format (NDJSON).

Both can be append-only, both can carry an integrity chain. The decision is which to default to.

## Decision

The default `AuditLogService` is `NdjsonAuditLogService`: one NDJSON file per `system_id`, append-only, every record carrying `prev_hmac` and `record_hmac` keyed on a secret only the writer knows. JSON keys are snake_case to match the Article 12 schema. Determinism of the SHA-256 input is preserved by an internal `ObjectMapper` that the starter does not expose to the application context (see ADR-0006).

A JDBC sink is a planned optional module for a future minor; the interface `AuditLogService` is open to it.

## Consequences

- Adoption cost on day one is zero database setup. The library writes to `${aiact.log-dir}` and the operator can ship the file to an evidence vault on a schedule.
- Tamper detection is an HTTP call (`/aiact/log/verify`). The verifier walks the file and reports the exact `event_id` list whose recomputed HMAC does not match the on-disk one.
- A retention prune creates a verifier "false positive" at the boundary record (the kept slice carries a `prev_hmac` whose predecessor was deleted). Documented in `RetentionPolicyServiceTest` and in the README operational notes.
- Multi-pod deployments require the `single-writer-lock` semantics (default ON): each append acquires an OS file lock, tails the file under the lock, recomputes the chain head from disk. On NFSv3 without lockd the lock is unreliable; the README points adopters at NFSv4 or a single-writer pod.

## Alternatives considered

**Postgres table by default.** Operationally heavier on day one (migration to apply, connection pool sized, retention TTL job), and the chain integrity story is the same (HMAC per row). Better as opt-in for adopters who already centralise audit in the DB.

**Append-only object storage (S3 with object-lock).** Strong tamper resistance from infrastructure, but introduces a hard cloud dependency and adds latency on every append. Rejected for v1; an `S3AuditLogService` is plausible as a separate optional module if a real adopter asks.

**Digital signatures per record (asymmetric).** Stronger than HMAC if the private key never leaves a trust boundary, but the operational cost (key generation, rotation, custody) is much higher and the threat model adoption-side rarely justifies it. The README threat model in `SECURITY.md` documents the trade-off.
