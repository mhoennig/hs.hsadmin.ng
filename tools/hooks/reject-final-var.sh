#!/usr/bin/env bash
set -uo pipefail

case "${1:-}" in
    -h|-\?|--help)
        source tools/lib/unindented.sh
        unindented <<'HELP'
            Usage: tools/hooks/reject-final-var.sh < hook-payload.json
            Check Java text added by Claude or Codex for forbidden 'final var' declarations.
HELP
        exit 0
        ;;
esac

payload=$(cat)

# Limit checks to added text so untouched legacy declarations stay silent.
if [[ "$(printf '%s' "$payload" | jq -r '.tool_name // ""')" == "apply_patch" ]]; then
    written=$(printf '%s' "$payload" | jq -r '.tool_input.command // ""' | awk '
        /^\*\*\* (Add|Update) File: / { java = ($0 ~ /\.java$/); next }
        /^\*\*\* Move to: / { java = ($0 ~ /\.java$/); next }
        /^\*\*\*/ { java = 0; next }
        java && /^\+/ { print substr($0, 2) }
    ')
else
    case "$(printf '%s' "$payload" | jq -r '.tool_input.file_path // ""')" in
        *.java) ;;
        *) exit 0 ;;
    esac
    written=$(printf '%s' "$payload" | jq -r '
        [.tool_input.content?, .tool_input.new_string?, (.tool_input.edits[]?.new_string?)]
        | map(select(. != null))
        | join("\n")')
fi

if printf '%s' "$written" | grep -qE '\bfinal[[:space:]]+var\b'; then
    echo "AGENTS.md violation: use Lombok 'val' instead of 'final var'." >&2
    echo "Fix the Java code just written and add 'import lombok.val;' if missing." >&2
    exit 2
fi
