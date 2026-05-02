# ADR-0004: HMAC chain instead of per-record digital signatures

- **Status:** accepted
- **Date:** 2026-04-29
- **Deciders:** Francesco Bilotta

## Context

Article 12 wants log integrity. There are two cryptographic shapes that achieve it:

- **HMAC chain (symmetric).** Every record is keyed with a secret known to the writer. The reader who has the same secret can verify. The chain links records: tampering a record breaks the chain at and after that point.
- **Digital signature per record (asymmetric).** Every record is signed with a private key held by the writer. Anyone with the public key can verify, the signing key can be hardware-bound (HSM / Secure Enclave), and the verifier never needs the secret.

Both detect tampering. They differ in operational cost and in threat surface.

## Decision

v1.0 ships HMAC chain. The secret is provided as `aiact.hmac.secret` (or `secret-ref` for vault adapters). The starter refuses to boot in production with the placeholder default secret; the dev profile tolerates it for local work.

## Consequences

- Adoption cost is one secret to manage. The same operational pattern any Spring Boot service already uses for DB credentials.
- An attacker who steals the HMAC secret can forge new records that verify. The threat model in `SECURITY.md` calls this out: HMAC protects against modification of past records assuming the key has not been compromised; it does not protect against an attacker with active write access plus the key.
- Key rotation requires a controlled re-seed: export the old chain, archive, start a new chain from `CHAIN_SEED` with the new secret. Documented in `docs/PRODUCTION.md`. The library does not pretend you can rotate transparently; rotation is an event with audit consequences.
- Wire format is simpler than a per-record signature: 32 hex bytes vs a multi-line PEM block. Easier for a notified body assessor to read by hand.

## Alternatives considered

**Digital signature per record with software key.** No real benefit over HMAC: the attacker who has runtime write access to forge records also has runtime read access to the software key.

**Digital signature per record with HSM-bound key.** Genuinely stronger: the signing key never leaves the HSM, so an attacker with full host compromise still cannot forge records. Operationally heavier (HSM provisioning, signing latency, HSM availability as a hard dependency on the request path). Plausible v2 add-on for adopters who already run HSMs; not a v1 default.

**Merkle tree over the file with periodic root anchoring on a public ledger.** Stronger property but very heavy operationally. Out of scope.

## Why this matters

HMAC is "good enough cryptography that fits the operational budget of a small Spring Boot team". Digital signatures with HSM are stronger but cost a vendor relationship and a hard runtime dependency. We picked the option an adopter can operate alone with one secret in their existing vault. The threat model in `SECURITY.md` documents what we accept by making this choice.

## References

- `HmacChain` and `HmacChain.CHAIN_SEED` in `spring-aiact-core/src/main/java/.../audit/`.
- Threat model: see `SECURITY.md` "Threat model (1-page summary)" section, rows S/T/R.
- Rotation procedure: `docs/PRODUCTION.md`.
