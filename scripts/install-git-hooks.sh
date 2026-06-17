#!/usr/bin/env bash
# Install the git hooks for this repo via the pre-commit framework
# (https://pre-commit.com): declarative config in .pre-commit-config.yaml, every
# tool version pinned, hook environments isolated. Stable entry point referenced
# by `make setup` and CONTRIBUTING.md.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if ! command -v pre-commit >/dev/null 2>&1 && [ ! -x ~/.local/bin/pre-commit ]; then
  echo "pre-commit not found. Install it:"
  echo "  pip3 install --user --break-system-packages pre-commit"
  exit 1
fi

PRE_COMMIT="$(command -v pre-commit || echo "$HOME/.local/bin/pre-commit")"

# Default stages (pre-commit, commit-msg) PLUS post-merge + post-rewrite.
# post-merge/post-rewrite regenerate the WHOLE-TREE _generated/* set after merge / pull /
# rebase / cherry-pick — operations that REPLAY existing commits and so never fire the
# pre-commit hook (ADR sw-scm-007). Without them, integrating several branches onto main
# yields an HEAD whose _generated matches no single branch and was regenerated against the
# union by nobody; the CI drift-gate then catches it late. --hook-type can be repeated.
# pre-commit + commit-msg (conventional commit gate) + the two integration stages.
"$PRE_COMMIT" install --hook-type pre-commit --hook-type commit-msg \
  --hook-type post-merge --hook-type post-rewrite
echo "git hooks installed (pre-commit framework, config: .pre-commit-config.yaml)"
echo "  stages: pre-commit, commit-msg, post-merge, post-rewrite"
