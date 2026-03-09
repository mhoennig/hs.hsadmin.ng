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
        int i = 0;
        while (i < line.length()) {
            final char c = line.charAt(i);
            if (c == ' ' || c == '\t') {
                i++;
            } else {
                break;
            }
        }

        val out = new StringBuilder(line.length() + 16);
        for (int j = 0; j < i; j++) {
            out.append(line.charAt(j) == '\t' ? "&nbsp;&nbsp;" : "&nbsp;");
        }

        for (int j = i; j < line.length(); j++) {
            final char c = line.charAt(j);
            if (c == '&') {
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '|') {
                out.append("&#124;");
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }

}
