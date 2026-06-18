# spring-aiact — local regen of the living requirements catalog (tracegate).
#
# The Java build stays on Maven (`./mvnw verify`); this Makefile only drives the
# tracegate dogfood loop so contributors can regenerate / drift-check the
# committed `*/_generated/` catalog the same way CI does.
#
#   make setup               install tracegate (pinned) + the pre-commit git hooks
#   make requirements        regenerate every module's _generated/ catalog
#   make requirements-check  drift-gate: exit 2 if the catalog drifted from the code
#   make tracegate-install   verify uv is present + warm the pinned-tracegate uvx cache
#
# The catalog is GENERATED — never hand-edit a `_generated/*` file. To change a
# requirement, change the test (rename it, add a @spec javadoc) and rerun `make
# requirements`.
.DEFAULT_GOAL := help

# Pin tracegate to the exact commit CI uses, so local and CI agree byte-for-byte.
TRACEGATE_REF ?= 3bb94964f1e0502d2e68681611bf5ac335180f4b
TRACEGATE_PKG := git+https://github.com/iambilotta/tracegate@$(TRACEGATE_REF)
# The pin footgun (sw-scm-005 made enforced): a bare `tracegate` resolves whatever the
# contributor last `pip install`-ed on PATH, which may differ from TRACEGATE_REF and produce
# FALSE drift against the committed _generated/ + the CI drift-gate's pinned generator. So EVERY
# invocation goes through uvx, which builds an ephemeral, exactly-pinned env from TRACEGATE_PKG
# (cached after the first run, no global pollution). Never call bare `tracegate` in a target/script.
TRACEGATE := uvx --from "$(TRACEGATE_PKG)" tracegate

.PHONY: help setup requirements requirements-check tracegate-install

help:  ## show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'

# One-shot per clone: ensure uv + warm the pinned-tracegate cache + install the pre-commit hooks.
# The hooks include the post-merge/post-rewrite regen stages that keep the whole-tree
# _generated/ catalog from drifting on merge/rebase/cherry-pick (ADR sw-scm-007).
setup: tracegate-install  ## ensure pinned tracegate (via uvx) + install the pre-commit git hooks
	scripts/install-git-hooks.sh

tracegate-install:  ## verify uv is present + warm the pinned-tracegate uvx cache (no global install)
	# tracegate is no longer installed globally (that was the pin footgun: a stale PATH
	# `tracegate` shadows the pin and produces FALSE drift). Every target/script runs it via
	# uvx, an ephemeral exactly-pinned env. This target only checks uv exists and warms the cache.
	@command -v uvx >/dev/null 2>&1 || { \
	  echo "uv/uvx not found. Install it: https://docs.astral.sh/uv/getting-started/installation/" >&2; \
	  exit 1; }
	$(TRACEGATE) --help >/dev/null

requirements:  ## regenerate the as-built requirements catalog (writes each module's _generated/)
	$(TRACEGATE) .

requirements-check:  ## drift-gate: fail (exit 2) if the committed catalog drifted from the code
	$(TRACEGATE) . --check
