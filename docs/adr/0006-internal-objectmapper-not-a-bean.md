# ADR-0006: The audit-record ObjectMapper is an implementation detail, not a Spring bean

- **Status:** accepted
- **Date:** 2026-05-02
- **Deciders:** Francesco Bilotta
- **Supersedes:** the v0.1.0 → v1.0.0 design that exposed `aiActObjectMapper` as a `@Bean`

## Context

The library serialises Article 12 audit records with a configured `ObjectMapper` (snake_case naming, `JavaTimeModule`, deterministic key order). The mapper is consumed by `PayloadHasher` to compute a stable SHA-256 of the input and the output, and by `NdjsonAuditLogService` to write the record to disk.

In v0.1.0 and v1.0.0 the mapper was a Spring bean of type `ObjectMapper`. This satisfied `@ConditionalOnMissingBean(ObjectMapper.class)` in Spring Boot's `JacksonAutoConfiguration`, with the consequence that Spring Boot stopped creating its own primary `ObjectMapper`. Spring MVC then picked up the only `ObjectMapper` in the context (ours, snake_case), and any user POST with camelCase JSON returned `400 Unrecognized field`. PR #1 patched it with `defaultCandidate=false`. That fix preserved a hidden coupling: a future Spring autoconfig could still inspect `aiActObjectMapper` by name and pull it in unintended ways.

## Decision

v1.0.0 onward: the audit-record mapper is **not** a Spring bean. It is built by a `private static ObjectMapper buildAuditEventMapper()` factory inside `AiActAutoConfiguration` and passed directly to `PayloadHasher` and `NdjsonAuditLogService` when those beans are constructed. `AiActLogController` no longer holds an `ObjectMapper` either: it streams Article 12 exports through the new `AuditLogService.writeJsonLine(Writer, AuditEvent)` default method, which the NDJSON implementation overrides.

`ObjectMapperIsolationTest` pins both halves of the contract: there is exactly one `ObjectMapper` bean in the context (Spring Boot's primary), and no bean named `aiActObjectMapper` at all. `PayloadHasherDeterminismTest` pins the SHA-256 output on a known payload so the refactor cannot regress the hash.

## Consequences

- No future Spring Boot autoconfig can shadow the user's `ObjectMapper` again, regardless of qualifier visibility flags. The risk is closed at the architectural level, not patched.
- Adopters who used the `@Qualifier("aiActObjectMapper")` to override hashing must now provide a `PayloadHasher` bean directly. The qualifier was never a documented extension point; the breaking change is documented in the v1.1.0 CHANGELOG with the migration line.
- The internal mapper is still configured for snake_case + ISO-8601 instants because Article 12 records and the SHA-256 input depend on that exact shape.

## Alternatives considered

**Keep the `defaultCandidate=false` patch from v0.1.1.** Lower migration cost but leaves the bean exposed by name. A future autoconfig that scans `ObjectMapper` beans by name (rare but legal) could still pull it. Not closed.

**Wrap the mapper in a domain type `record AiActAuditMapper(ObjectMapper delegate)` and bean-publish that.** Solves the shadowing issue but introduces a wrapper class with no other purpose. Pure ceremony.

**Keep the bean and document the qualifier as the override extension.** Doubles down on the wrong shape; the audit-record mapper is not a logical extension point.
