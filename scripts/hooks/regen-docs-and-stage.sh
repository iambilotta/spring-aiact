#!/usr/bin/env bash
# Regenerate every module's _generated/ catalog and auto-stage the results.
# Idempotent (clean tree -> byte-identical output -> `git add` is a no-op).
# Owned by .pre-commit-config.yaml; do not call directly (use `make requirements` for that).
#
# The generator is the OSS tool `tracegate` (github.com/iambilotta/tracegate), installed
# pinned by `make setup` (-> make tracegate-install). The pin is the single source of the
# version (Makefile TRACEGATE_REF, kept in lockstep with the CI tracegate job). Running it
# on every commit AND after every tree-mutating integration op (post-merge / post-rewrite)
# is what makes the committed catalog unable to drift (ADR sw-scm-007).
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

if ! command -v tracegate >/dev/null 2>&1; then
  echo "tracegate not found on PATH. Run 'make setup' (or 'make tracegate-install') once per clone." >&2
  exit 1
fi

# `tracegate .` auto-detects the test-bearing modules (overrides in tracegate.toml) and
# writes each module's _generated/ in place. Same command the CI drift-gate runs with --check.
tracegate . >/dev/null

# Stage every generated artifact across all modules EXCEPT gitignored ones (none today; the
# guard keeps the hook from dying under `set -e` if a runtime-dependent doc is added later).
while IFS= read -r -d '' f; do
  git check-ignore -q "$f" && continue
  git add "$f"
done < <(find . -type f -path '*/_generated/*' -print0)
