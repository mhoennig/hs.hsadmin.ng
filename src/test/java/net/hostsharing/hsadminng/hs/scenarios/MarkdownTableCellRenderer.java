package net.hostsharing.hsadminng.hs.scenarios;

import lombok.experimental.UtilityClass;
import lombok.val;

@UtilityClass
public class MarkdownTableCellRenderer {

    public static String toMarkdownTableCell(final Object value) {
        val raw = String.valueOf(value);
        if (raw.isEmpty()) {
            return "";
        }

        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n");
        val lines = normalized.split("\n", -1);
        val out = new StringBuilder(normalized.length() + (lines.length * 4));

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append("<br>");
            }
            out.append(escapeWithIndent(lines[i]));
        }

        return out.toString();
    }

    private String escapeWithIndent(final String line) {
        val indentEnd = indentEnd(line);
        val out = new StringBuilder(line.length() + 16);
        appendIndentAsNbsp(out, line, indentEnd);
        appendEscaped(out, line, indentEnd);
        return out.toString();
    }

    private int indentEnd(final String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    private void appendIndentAsNbsp(final StringBuilder out, final String line, final int indentEnd) {
        for (int j = 0; j < indentEnd; j++) {
            out.append(line.charAt(j) == '\t' ? "&nbsp;&nbsp;" : "&nbsp;");
        }
    }

    private void appendEscaped(final StringBuilder out, final String line, final int fromIndex) {
        for (int j = fromIndex; j < line.length(); j++) {
            final char c = line.charAt(j);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '|' -> out.append("&#124;");
                default -> out.append(c);
            }
        }
    }

}
