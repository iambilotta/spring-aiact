# ADR-0008: Live encryption-at-rest deferred to v1.2.x; filesystem encryption is the v1.x answer

- **Status:** proposed
- **Date:** 2026-05-02
- **Deciders:** Francesco Bilotta

## Context

`AiActProperties.Encryption` exists as a placeholder since v0.1.0, and `aiact.encryption.enabled=true` parses without effect. The README documents the gap and points adopters at filesystem-level encryption (LUKS, EBS-encrypted, EFS-at-rest) for v1.x.

The question is whether v1.2 should promote encryption-at-rest from "placeholder + filesystem mitigation" to "library-managed AES-GCM per record with key rotation".

## Decision

Stay deferred for v1.2. Document the design space publicly in `docs/ENCRYPTION.md` (separate document) so adopters can comment, and so the maintainer's bandwidth is committed to the design only after a real adopter requests it. Do not ship code yet.

## Why this matters

Encryption-at-rest is the kind of feature that looks like a one-week sprint and turns into a quarter once you wire key rotation, KMS adapters, performance impact on the append path, and the interplay with the HMAC chain. Filesystem encryption is "good enough for the threat model the README documents", and the cost of overshooting the implementation is much higher than the cost of carrying the placeholder property forward.

## Consequences

- v1.2 ships without live encryption-at-rest.
- Adopters in regulated environments who already run encrypted filesystems are unaffected.
- Adopters who need application-level encryption (multi-tenant SaaS where the host operator is also a threat) get a documented deferral, not a half-built feature.
- `aiact.encryption.enabled=true` keeps parsing without effect; the property is documented as placeholder. Removing it would break backwards compatibility for nothing.

## Alternatives considered

**Ship a minimum-viable AES-GCM per record in v1.2.** Plausible but the operational fold (key rotation, KMS provider abstraction, recovery from a partially-rotated chain) is not 80/20.

**Drop the placeholder property in v2.0.** Considered. Will be done if `docs/ENCRYPTION.md` reaches a different conclusion and we ship a real implementation under a different property name.

## References

- Placeholder property: `AiActProperties.Encryption` in `spring-aiact-core/src/main/java/.../config/`.
- Threat model: `SECURITY.md` row I (Information disclosure).
- Design discussion: `docs/ENCRYPTION.md` (to be added).
