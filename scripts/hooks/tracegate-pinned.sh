# shellcheck shell=bash
# Resolve the PINNED tracegate invocation for shell hooks/scripts.
#
# The pin footgun (sw-scm-005 made enforced, RFC 0024 sibling): a bare `tracegate` resolves
# whatever the contributor last `pip install`-ed on PATH. If that differs from TRACEGATE_REF,
# the regen produces output that disagrees with the committed _generated/ AND with CI's pinned
# drift-gate -> FALSE drift. So scripts NEVER call bare `tracegate`; they call the array
# `TRACEGATE[@]` defined here, which runs the generator via `uvx` in an ephemeral env pinned
# to TRACEGATE_REF (cached after the first run, no global pollution).
#
# Single source of the pin: the Makefile's `TRACEGATE_REF`. We parse it from there so the hook,
# the Makefile and the CI job cannot drift apart (the failure mode this file exists to kill).
#
# Usage:
#   source "$(dirname "$0")/tracegate-pinned.sh"   # defines the TRACEGATE array
#   "${TRACEGATE[@]}" .            # regen
#   "${TRACEGATE[@]}" . --check    # drift-gate
set -euo pipefail

_TG_ROOT="$(git rev-parse --show-toplevel)"

# Read TRACEGATE_REF from the Makefile (single source of truth). `?=` or `:=` form.
TRACEGATE_REF="$(sed -n 's/^TRACEGATE_REF[[:space:]]*[?:]\{0,1\}=[[:space:]]*\([0-9a-f]\{7,\}\).*/\1/p' "$_TG_ROOT/Makefile" | head -n1)"
if [ -z "${TRACEGATE_REF:-}" ]; then
  echo "FATAL: could not read TRACEGATE_REF from $_TG_ROOT/Makefile (the single source of the pin)." >&2
  exit 1
fi
TRACEGATE_PKG="git+https://github.com/iambilotta/tracegate@${TRACEGATE_REF}"

if ! command -v uvx >/dev/null 2>&1; then
  echo "FATAL: uv/uvx not found. tracegate runs ONLY through the pinned uvx env (no global install)." >&2
  echo "Install uv: https://docs.astral.sh/uv/getting-started/installation/ then re-run 'make setup'." >&2
  exit 1
fi

# The pinned invocation. Always exactly TRACEGATE_REF; a stale PATH `tracegate` is never used.
# Consumed by the scripts that source this file (SC2034: used externally, not in this file).
# shellcheck disable=SC2034
TRACEGATE=(uvx --from "${TRACEGATE_PKG}" tracegate)
