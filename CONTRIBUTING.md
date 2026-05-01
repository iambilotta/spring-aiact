# Contributing

Thank you for considering a contribution to `spring-aiact`. The project is small, opinionated
and deadline-bound (the high-risk obligations of the EU AI Act take effect on 2 August 2026).
Keep that in mind when you propose changes.

## What is in scope

- Bug fixes in the audit log, the HMAC chain, the generators, the Maven plugin.
- New official Annex III sub-points, when the regulation evolves.
- Documentation that improves the path from annotation to evidence pack.
- Test coverage that hardens the audit log against tampering.

## What is out of scope (for now)

- Replacements of the JCA / Spring AOP / Maven foundations.
- Vendor specific persistence backends. Submit them as separate libraries that implement
  `AuditLogService`.
- Custom Annex III categories. The enum mirrors the regulation; do not invent values.
- Certification claims. The starter is evidence-as-code, not a notified body.

## Local development

```bash
sdk use java 21.0.11-amzn
mvn clean test
```

The full reactor builds in well under a minute on a modern laptop. Any failing test must be
fixed before opening a pull request.

## Coding standards

- Java 21 release target, no preview features.
- No third-party dependencies in `spring-aiact-core` beyond Spring, Jackson and SLF4J.
- The generators must never invent text. When data is missing, render a visible placeholder.
- The HMAC chain is the load-bearing security primitive. Any change to the canonical
  serialization of `AuditEvent` must be accompanied by a tampering test.

## Reporting an AI Act misalignment

If you discover that a generated artifact disagrees with the AI Act text, open an issue with
the article reference and a short reproduction. AI Act misalignments take priority over feature
requests.
