package net.hostsharing.hsadminng.rbac.rbacdef;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class StringWriter {

    private final StringBuilder string = new StringBuilder();
    private int indentLevel = 0;

    static VarDef with(final String var, final String name) {
        return new VarDef(var, name);
    }

    void writeLn(final String text) {
        string.append( indented(text));
        writeLn();
    }

    void writeLn(final String text, final VarDef... varDefs) {
        string.append( indented( new VarReplacer(varDefs).apply(text) ));
        writeLn();
    }

    void writeLn() {
        string.append( "\n");
    }

    void indent() {
        ++indentLevel;
    }

    void unindent() {
        --indentLevel;
    }

    void indented(final Runnable indented) {
        indent();
        indented.run();
        unindent();
    }

    boolean chopTail(final String tail) {
        if (string.toString().endsWith(tail)) {
            string.setLength(string.length() - tail.length());
            return true;
        }
        return false;
    }

    void chopEmptyLines() {
        while (string.toString().endsWith("\n\n")) {
            string.setLength(string.length() - 1);
        };
    }

    void ensureSingleEmptyLine() {
        chopEmptyLines();
        writeLn();
    }

    @Override
    public String toString() {
        return string.toString();
    }

    public static String indented(final String text, final int indentLevel) {
        final var indentation = StringUtils.repeat("    ", indentLevel);
        final var indented = stream(text.split("\n"))
                .map(line -> line.trim().isBlank() ? "" : indentation + line)
                .collect(joining("\n"));
        return indented;
    }

    private String indented(final String text) {
        if ( indentLevel == 0) {
            return text;
        }
        return indented(text, indentLevel);
    }

    record VarDef(String name, String value){}

    private static final class VarReplacer {

        private final VarDef[] varDefs;
        private String text;

        private VarReplacer(VarDef[] varDefs) {
            this.varDefs = varDefs;
        }

        String apply(final String textToAppend) {
                try {
                    text = textToAppend;
                    stream(varDefs).forEach(varDef -> {
                        final var pattern = Pattern.compile("\\$\\{" + varDef.name() + "}", Pattern.CASE_INSENSITIVE);
                        final var matcher = pattern.matcher(text);
                        text = matcher.replaceAll(varDef.value());
                    });
                    return text;
                } catch (Exception exc) {
                    throw exc;
                }
            }
        }
}
