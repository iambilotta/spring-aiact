# Architecture Decision Records

Each file under `docs/adr/` records a non-trivial design decision in a fixed format ([Michael Nygard's ADR template](https://www.cognitect.com/blog/2011/11/15/documenting-architecture-decisions)). Status, date, context, decision, consequences, alternatives. Short.

The point is honesty: a future contributor (or future me) can re-evaluate a decision with the full context that produced it, instead of guessing from the code.

## Index

- [ADR-0001](0001-annotations-as-source-of-truth.md): Annotations as the source of truth for AI Act evidence.
- [ADR-0002](0002-ndjson-hmac-chain-vs-database.md): NDJSON file with HMAC chain as the default Article 12 sink.
- [ADR-0003](0003-spring-aop-advisor.md): Spring AOP advisor on `@AiActLog` instead of bytecode rewriting.
- [ADR-0004](0004-hmac-vs-digital-signature.md): HMAC chain instead of per-record digital signatures.
- [ADR-0005](0005-jitpack-distribution-v1.md): JitPack as the canonical distribution for v1.x; Maven Central deferred.
- [ADR-0006](0006-internal-objectmapper-not-a-bean.md): The audit-record `ObjectMapper` is an implementation detail, not a Spring bean.
- [ADR-0007](0007-typed-chain-head-record.md): `AuditLogService.head()` returns `ChainHead` record, not `Map<String, String>`.

## How to add an ADR

1. Copy `0000-template.md` to the next available number, kebab-case the title.
2. Set status to `proposed`. Open a PR. After review, flip to `accepted`.
3. If a later ADR supersedes an earlier one, reference both ways (`Superseded by`, `Supersedes`).
4. Never delete an old ADR. Mark it `superseded` or `deprecated` and link the replacement.
