# Defines the `normalize_plantuml_fences` filter, sourced from the repo root by the tools which
# render Markdown containing PlantUML diagrams.
#
# `normalize_plantuml_fences <input-file> <output-file>` rewrites the PlantUML fences of a Markdown
# file into what the `plantuml` command accepts: it appends a missing `@enduml`, and it replaces the
# colons inside identifiers such as `entity contact:OWNER` by underscores, because PlantUML treats a
# colon as a label separator.

normalize_plantuml_fences() {
    local input_file=$1
    local output_file=$2

    awk '
        function sanitize_plantuml_identifier(identifier) {
            gsub(/:/, "_", identifier)
            return identifier
        }

        function sanitize_plantuml_references(line,   i, ch, previous, next_char, result, in_quote) {
            result = ""
            in_quote = 0

            for (i = 1; i <= length(line); i++) {
                ch = substr(line, i, 1)
                if (ch == "\"") {
                    in_quote = !in_quote
                }
                if (!in_quote && ch == ":") {
                    previous = substr(line, i - 1, 1)
                    next_char = substr(line, i + 1, 1)
                    if (previous ~ /[[:alnum:]_]/ && next_char ~ /[[:alnum:]_]/) {
                        ch = "_"
                    }
                }
                result = result ch
            }

            return result
        }

        function normalize_plantuml_line(line,   prefix, identifier, alias) {
            if (line ~ /^[[:space:]]*(entity|object|actor|class|enum)[[:space:]]+[A-Za-z_][A-Za-z0-9_]*:[A-Za-z0-9_:]*[[:space:]]*$/) {
                prefix = line
                sub(/[A-Za-z_][A-Za-z0-9_]*:[A-Za-z0-9_:]*[[:space:]]*$/, "", prefix)
                identifier = line
                sub(/^[[:space:]]*(entity|object|actor|class|enum)[[:space:]]+/, "", identifier)
                sub(/[[:space:]]*$/, "", identifier)
                alias = sanitize_plantuml_identifier(identifier)
                return prefix "\"" identifier "\" as " alias
            }

            if (line ~ /[[:space:]]as[[:space:]]/ || line ~ /[-.xo*]+[udlr]?[.=-]*[<>-]+/ || line ~ /[<>-]+[.=-]*[udlr]?[-.xo*]+/) {
                return sanitize_plantuml_references(line)
            }

            return line
        }

        /^[[:space:]]*```[[:space:]]*plantuml[[:space:]]*$/ {
            in_plantuml = 1
            has_start = 0
            has_end = 0
            print
            next
        }

        in_plantuml && /^[[:space:]]*```[[:space:]]*$/ {
            if (has_start && !has_end) {
                print "@enduml"
            }
            in_plantuml = 0
            print
            next
        }

        in_plantuml && /^[[:space:]]*@start/ {
            has_start = 1
        }

        in_plantuml && /^[[:space:]]*@end/ {
            has_end = 1
        }

        in_plantuml {
            print normalize_plantuml_line($0)
            next
        }

        {
            print
        }
    ' "${input_file}" > "${output_file}"
}
