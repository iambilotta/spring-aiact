# Encryption-at-rest design note

> **Status:** design study, no code shipped. See [ADR-0008](adr/0008-encryption-at-rest-deferred.md) for the decision to defer to v1.2.x.

`AiActProperties.Encryption` exists as a placeholder since v0.1.0 and `aiact.encryption.enabled=true` parses without effect. The current v1.x answer for at-rest confidentiality is **filesystem encryption** (LUKS, EBS-encrypted volume, EFS-at-rest). This document records the design space for a future library-managed mode, so when an adopter brings the requirement we are not starting from zero.

## Why filesystem encryption is the v1.x answer

For the threat model in `SECURITY.md`, filesystem encryption is the right primitive:

- the host operator is trusted (the audit chain assumes this anyway: an attacker with root can delete the file regardless of cipher),
- the cloud provider is trusted at the volume layer,
- the threat being addressed is "stolen disk", which a vanilla EBS-encrypted volume already covers,
- zero implementation cost.

Library-managed encryption adds value only in scenarios where the host operator is **not** trusted: multi-tenant SaaS where the same kernel serves multiple customers and one tenant's audit must be unreadable to the SaaS team itself. We do not have that adopter yet.

## Design space

### Granularity

Three options:

| Granularity | Pro | Con |
|---|---|---|
| Per-file (one ciphertext per `system_id`.ndjson) | Simplest, full file is the encryption unit. Same key for the lifetime of the file. | Whole file decrypted to read or verify. Append needs to re-encrypt the file or use a streaming cipher mode. |
| Per-record (each NDJSON line is a ciphertext) | Append + verify can stream a single record. Each record can carry its own IV. | Larger overhead (IV + auth tag per record). HMAC chain needs to operate on plaintext or ciphertext, decision required (see below). |
| Per-block (group of N records) | Compromise between the two. | Boundary records share state; harder to reason about. |

**Leaning toward per-record.** The append/verify path stays streaming, the per-record overhead (12B IV + 16B GCM tag = 28B) is cheap compared to a 250B record, and the HMAC chain can keep operating on ciphertext (see next section).

### HMAC chain interaction

The HMAC chain in v1.x is computed over the canonical record JSON. With encryption at rest:

- **Option A: HMAC over ciphertext.** The chain is computed over the ciphertext blob written to disk. Verify reads the file, recomputes HMAC over each ciphertext record. Decryption is only needed to render the record content (export endpoint), not to verify integrity. Clean separation.
- **Option B: HMAC over plaintext.** The chain stays as-is (plaintext bytes), and the file stores ciphertext separately or with the HMAC alongside. Verify must decrypt to recompute. Couples confidentiality and integrity unnecessarily.

**Leaning toward A.** Verify never requires the encryption key. An attacker with the HMAC key can still verify, an attacker with the encryption key can still read; orthogonal compromises stay orthogonal.

### Cipher

AES-GCM-256 with 96-bit IV. Standard, JCE-supported, authenticated.

Avoid AES-CBC because of the IV-related pitfalls and because GCM gives authentication for free. Avoid ChaCha20-Poly1305 unless the deployment target is constrained (mobile, embedded), which is not the AI Act scenario.

### Key management

The hard part. Three layers:

1. **Data encryption key (DEK)**: AES-256, used to encrypt records.
2. **Key encryption key (KEK)**: encrypts the DEK at rest. Lives in a vault (HashiCorp Vault, AWS KMS, GCP KMS, Azure Key Vault).
3. **DEK rotation**: optional, on a calendar or volume-based schedule. New DEK starts a new chain segment; old DEK keeps decrypting old records.

Adopter-side configuration:

```yaml
aiact:
  encryption:
    enabled: true
    key-source: vault          # vault | kms-aws | kms-gcp | kms-azure | static
    key-id: secret/aiact-dek   # interpreted by the source
    rotation: P90D             # optional; null = no rotation
```

A `KeyMaterial` SPI lets the library defer to the user's vault adapter without baking in a hard dependency on any vault.

### Performance impact

AES-GCM in modern JVMs (with AES-NI) hashes ~1 GB/s. For a 250-byte record the encryption cost is ~250 ns plus the GCM tag computation: well under the existing HMAC chain cost. Probably immeasurable next to the file lock + fsync.

This is the easy news. The hard news is that we would need to JMH-pin it before claiming.

### Key rotation interplay with HMAC chain

Two events at v1.x are documented:

- **HMAC secret rotation**: export old chain, archive, re-seed (`docs/PRODUCTION.md`).
- **Retention prune**: kept slice carries `prev_hmac` whose predecessor is gone; verify reports a boundary mismatch (`RetentionPolicyServiceTest`).

A **DEK rotation** in encryption-at-rest mode is a third event of the same shape. The kept records under the old DEK are still decryptable with the old DEK (which the library archives). The new chain segment starts under the new DEK with a fresh `CHAIN_SEED`.

Documenting this honestly: rotation is **always** an event. The library does not pretend you can rotate transparently.

## Adoption path

Library-managed encryption is a v1.2.x candidate. Trigger conditions:

- An adopter explicitly asks for it with a non-trusted-host threat model.
- We ship one vault adapter (Vault probably, since it is the lowest-effort and most ecosystem-friendly).
- We ship a JMH benchmark proving the encryption overhead is in the noise (less than the HMAC chain cost).

Without all three, the library carries the placeholder forward. The placeholder is documented as such and the README points adopters at filesystem encryption for v1.x.

## What would change in code

Sketched, not committed:

- New `AuditRecordCipher` interface in `spring-aiact-core`, default no-op.
- `AesGcmRecordCipher` implementation in a new optional module `spring-aiact-encryption-aes-gcm`.
- `NdjsonAuditLogService.append` calls `cipher.encrypt(serialize(event))` before writing the line and computing HMAC; `stream` calls `cipher.decrypt` before deserializing.
- `KeyMaterial` SPI for vault adapters.
- New tests pinning round-trip (encrypt → write → read → decrypt → verify chain).

The `AiActProperties.Encryption` placeholder configuration property tree is the contract surface; live encryption fills it in without renaming.

## Out of scope

- Field-level encryption inside the audit record (encrypt only `userIdPseudonymized` while leaving other fields plaintext). Not asked for, marginal value, and the `MetadataSanitizer` already strips PII from the audit metadata.
- Hardware-backed signing. Already discussed in [ADR-0004](adr/0004-hmac-vs-digital-signature.md).
- Forward-secrecy via per-day key derivation. Out of scope for v1.x, possibly v1.3.

## Decision restated

v1.2.x will revisit. Until then, filesystem encryption + the HMAC chain + the threat model in `SECURITY.md` is the v1.x story. Documented, honest, not under-claimed and not over-promised.
