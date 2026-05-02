# Support

## Where to get help

| You want to | Go to |
|---|---|
| Report a bug or unexpected behaviour | [Issues](https://github.com/iambilotta/spring-aiact/issues/new/choose) |
| Suggest a feature, new annotation, new endpoint | [Issues](https://github.com/iambilotta/spring-aiact/issues/new/choose) |
| Ask whether the library is the right fit for your AI Act scenario | [Discussions](https://github.com/iambilotta/spring-aiact/discussions) |
| Ask for AI Act regulation interpretation | [Discussions](https://github.com/iambilotta/spring-aiact/discussions). The library does not give legal advice; the maintainer is happy to point you to relevant articles and to publicly available guidance, not to your DPO. |
| Report a vulnerability | [Private security advisory](https://github.com/iambilotta/spring-aiact/security/advisories/new). Do NOT open a public issue. See [SECURITY.md](SECURITY.md). |
| Commercial support, integration help, custom adapters | francesco@iambilotta.com |

## Response times

This is an open-source project maintained by one engineer in Europe. SLAs are best-effort:

- **Security advisories**: triaged within 5 working days (faster if an active CVE is involved).
- **Bugs with a clean reproducer**: triaged within 7 days.
- **Feature requests**: triaged when the maintainer has bandwidth, no fixed window.
- **Discussions**: best-effort, usually within a week.

If you need a guaranteed response time, that is what `francesco@iambilotta.com` is for.

## Before opening an issue

- Verify the bug is reproducible against `v1.1.0` or later.
- Check [existing issues](https://github.com/iambilotta/spring-aiact/issues?q=) for duplicates.
- Read the [README](README.md) and the [ADRs](docs/adr/) (some bug reports are answered there).
- Strip secrets from your reproducer.

## Versioning and breaking changes

The project follows [Semantic Versioning](https://semver.org/) from v1.0 onward. Breaking changes ship in major bumps and are documented with a migration block in [CHANGELOG.md](CHANGELOG.md). The `0.x` series predates the API freeze; do not assume API stability if you are pinned to a 0.x.

## Out of scope

The maintainer will not:

- Audit your AI Act conformity assessment for you.
- Speak to your notified body on your behalf.
- Backport fixes to discontinued versions or to Spring Boot 2.x.
- Build adapters for non-Spring frameworks as a free request.
