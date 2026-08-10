#!/usr/bin/env bash
# Claude-Code PostToolUse hook: rejects `final var` in freshly written Java code.
#
# AGENTS.md mandates Lombok `val`, but much of the legacy code still uses `final var`,
# which is easy to imitate while editing such a file; this catches it at write time.
set -uo pipefail

payload=$(cat)

case "$(printf '%s' "$payload" | jq -r '.tool_input.file_path // ""')" in
    *.java) ;;
    *) exit 0 ;;
esac

# only the text just written, so untouched legacy code in the same file stays silent
written=$(printf '%s' "$payload" | jq -r '
    [.tool_input.content?, .tool_input.new_string?, (.tool_input.edits[]?.new_string?)]
    | map(select(. != null))
    | join("\n")')

# word boundaries, thus "the final variable name" is no false positive
if printf '%s' "$written" | grep -qE '\bfinal[[:space:]]+var\b'; then
    echo "AGENTS.md violation: use Lombok 'val' instead of 'final var'." >&2
    echo "Fix the Java code just written and add 'import lombok.val;' if missing." >&2
    exit 2
fi
