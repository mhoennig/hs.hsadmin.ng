# Defines the `unindented` filter, sourced from the repo root by tools that show a -h/--help page.
#
# `unindented` reads text from stdin and removes the first line's indentation from every line, so a
# heredoc can stay indented in the source but still print flush-left. This replaces `<<-`, which
# strips only tabs and thus breaks under the repo's space-only .editorconfig.
#
#   unindented <<'HELP'
#           usage: ...
#   HELP
#
# TODO.tool: once Bash >= 5.3 is the baseline on all target systems (incl. the servers),
#   its `<<~EOF` heredocs strip the common indentation natively (even allowing an indented
#   end-marker), so this filter can be dropped.
#   Once we can use that version, we should use `<<~EOF` heredocs everywhere,
#   not just where `unindented` is used.

unindented() {
    awk 'NR==1 { indent = match($0, /[^ ]/) - 1 } { print substr($0, indent + 1) }'
}
