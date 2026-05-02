# ADR-0007: AuditLogService.head() returns ChainHead record, not Map<String, String>

- **Status:** accepted
- **Date:** 2026-05-02
- **Deciders:** Francesco Bilotta

## Context

`GET /aiact/log/head` returns the current head HMAC of the chain for a given `system_id`. Adopters poll this endpoint as a tamper canary. Up to v1.0.0 the controller computed the head with an `AtomicReference<String>` walked over a `Stream<AuditEvent>`, and returned a `Map.of("system_id", ..., "head_hmac", ...)`.

Two smells:

- the walk lived in the controller, not in `AuditLogService`. Concrete sinks that can answer in O(1) (a database with an indexed `MAX(record_hmac) WHERE system_id = ?`) had no way to override.
- the response was an untyped map. OpenAPI schema was lossy, Java callers consumed it as `Map<String, String>` and lost compile-time field guarantees.

## Decision

v1.1.0:

- New API `AuditLogService.head(String systemId)` returning `record ChainHead(String systemId, String headHmac)`. Default implementation walks the stream; concrete sinks can override for O(1).
- `AiActLogController.head()` collapses to two lines and returns `ChainHead` directly.
- JSON wire compatibility preserved via `@JsonProperty("system_id")` and `@JsonProperty("head_hmac")`. Existing JSON consumers see no change. Java consumers that referenced `Map<String, String>` migrate to `ChainHead`.

## Consequences

- The OpenAPI schema for `GET /aiact/log/head` is now a typed object, not a free-form map. Better adopter DX.
- Future sinks (JDBC, Redis, etc) can answer the head query in O(1) without touching the controller.
- One-line breaking change for Java callers of the controller method directly. JSON consumers unaffected. Migration documented in the v1.1.0 CHANGELOG.

## Alternatives considered

**Keep the map response, add the SPI method only.** Half-measure. The smell of returning `Map<String, String>` from a controller is independent of the SPI shape.

**Return the entire `AuditEvent` of the head record.** Considered but the `AuditEvent` is heavy (~17 fields), most of them irrelevant to a "where is the head" query. The two fields callers actually use are the system id and the head HMAC.
