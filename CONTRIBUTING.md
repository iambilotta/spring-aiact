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

### Git hooks (one-off)

```bash
make setup   # installs the pinned tracegate + the pre-commit framework hooks
```

This installs the [pre-commit](https://pre-commit.com) hooks (config in
`.pre-commit-config.yaml`, every tool version pinned). They keep the committed
`*/_generated/` catalog from drifting:

- **pre-commit** regenerates the catalog and auto-stages it on every commit.
- **post-merge / post-rewrite** regenerate it again after `merge` / `pull` / `rebase` /
  `cherry-pick` — those operations replay existing commits and bypass the pre-commit hook,
  so without these stages an integrated HEAD can drift from what any single branch committed
  (the CI `tracegate` gate would then catch it late). The same pinned generator runs in all
  three places, so local and CI agree byte-for-byte.

You can skip the hooks for a single commit with `pre-commit`'s native mechanism
(e.g. `SKIP=regen-generated-docs git commit ...`); CI still drift-gates the catalog.

### Living requirements (tracegate)

The per-module `_generated/` catalog is produced by
[tracegate](https://github.com/iambilotta/tracegate) from the test suite and is
**generated, never hand-edited**. If a change touches tests, regenerate the catalog and
commit it; CI drift-gates it (the `tracegate` job) and will fail the PR if the committed
catalog disagrees with the code:

```bash
make tracegate-install   # one-off
make requirements        # regenerate the catalog
make requirements-check  # what CI runs (exit 2 on drift)
```

To change a requirement, change the test (rename it, or add a `@spec.given` /
`@spec.when` / `@spec.then` javadoc), then `make requirements`. See
[`tracegate.toml`](tracegate.toml) to register a new test-bearing module.

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
