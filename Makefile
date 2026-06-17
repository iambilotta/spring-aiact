# spring-aiact — local regen of the living requirements catalog (tracegate).
#
# The Java build stays on Maven (`./mvnw verify`); this Makefile only drives the
# tracegate dogfood loop so contributors can regenerate / drift-check the
# committed `*/_generated/` catalog the same way CI does.
#
#   make setup               install tracegate (pinned) + the pre-commit git hooks
#   make requirements        regenerate every module's _generated/ catalog
#   make requirements-check  drift-gate: exit 2 if the catalog drifted from the code
#   make tracegate-install   install tracegate, pinned to the CI commit
#
# The catalog is GENERATED — never hand-edit a `_generated/*` file. To change a
# requirement, change the test (rename it, add a @spec javadoc) and rerun `make
# requirements`.
.DEFAULT_GOAL := help

# Pin tracegate to the exact commit CI uses, so local and CI agree byte-for-byte.
TRACEGATE_REF ?= 3bb94964f1e0502d2e68681611bf5ac335180f4b
TRACEGATE_PKG := git+https://github.com/iambilotta/tracegate@$(TRACEGATE_REF)
PIP ?= python3 -m pip

.PHONY: help setup requirements requirements-check tracegate-install

help:  ## show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'

# One-shot per clone: pin tracegate (the generator) + install the pre-commit framework hooks.
# The hooks include the post-merge/post-rewrite regen stages that keep the whole-tree
# _generated/ catalog from drifting on merge/rebase/cherry-pick (ADR sw-scm-007).
setup: tracegate-install  ## install tracegate (pinned) + the pre-commit git hooks (run once per clone)
	scripts/install-git-hooks.sh

tracegate-install:  ## install tracegate, pinned to the CI commit
	$(PIP) install --quiet "$(TRACEGATE_PKG)"

requirements:  ## regenerate the as-built requirements catalog (writes each module's _generated/)
	tracegate .

requirements-check:  ## drift-gate: fail (exit 2) if the committed catalog drifted from the code
	tracegate . --check
