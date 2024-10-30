package net.hostsharing.hsadminng.hs.office.scenarios;

import java.util.Map;

public class TemplateResolver {

    private final String template;
    private final Map<String, Object> properties;
    private final StringBuilder resolved = new StringBuilder();
    private int position = 0;

    public TemplateResolver(final String template, final Map<String, Object> properties) {
        this.template = template;
        this.properties = properties;
    }

    String resolve() {
        copy();
        return resolved.toString();
    }

    private void copy() {
        while (hasMoreChars()) {
            if ((currentChar() == '$' || currentChar() == '%') && nextChar() == '{') {
                startPlaceholder(currentChar());
            } else {
                resolved.append(fetchChar());
            }
        }
    }

    private boolean hasMoreChars() {
        return position < template.length();
    }

    private void startPlaceholder(final char intro) {
        skipChars(intro + "{");
        int nested = 0;
        final var placeholder = new StringBuilder();
        while (nested > 0 || currentChar() != '}') {
            if (currentChar() == '}') {
                --nested;
                placeholder.append(fetchChar());
            } else if ((currentChar() == '$' || currentChar() == '%') && nextChar() == '{') {
                ++nested;
                placeholder.append(fetchChar());
            } else {
                placeholder.append(fetchChar());
            }
        }
        final var name = new TemplateResolver(placeholder.toString(), properties).resolve();
        final var value = propVal(name);
        if ( intro == '%') {
            resolved.append(value);
        } else {
            resolved.append(optionallyQuoted(value));
        }
        skipChar('}');
    }

    private Object propVal(final String name) {
        final var val = properties.get(name);
        if (val == null) {
            throw new IllegalStateException("Missing required property: " + name);
        }
        return val;
    }

    private void skipChar(final char expectedChar) {
        if (currentChar() != expectedChar) {
            throw new IllegalStateException("expected '" + expectedChar + "' but got '" + currentChar() + "'");
        }
        ++position;
    }

    private void skipChars(final String expectedChars) {
        final var nextChars = template.substring(position, position + expectedChars.length());
        if ( !nextChars.equals(expectedChars) ) {
            throw new IllegalStateException("expected '" + expectedChars + "' but got '" + nextChars + "'");
        }
        position += expectedChars.length();
    }

    private char fetchChar() {
        if ((position+1) > template.length()) {
            throw new IllegalStateException("no more characters. resolved so far: " + resolved);
        }
        final var currentChar = currentChar();
        ++position;
        return currentChar;
    }

    private char currentChar() {
        if (position >= template.length()) {
            throw new IllegalStateException("no more characters, maybe closing bracelet missing in template: '''\n" + template + "\n'''");
        }
        return template.charAt(position);
    }

    private char nextChar() {
        if ((position+1) >= template.length()) {
            throw new IllegalStateException("no more characters. resolved so far: " + resolved);
        }
        return template.charAt(position+1);
    }

    private static String optionallyQuoted(final Object value) {
        return switch (value) {
            case Boolean bool -> bool.toString();
            case Number number -> number.toString();
            case String string -> "\"" + string.replace("\n", "\\n") + "\"";
            default -> "\"" + value + "\"";
        };
    }

    public static void main(String[] args) {
        System.out.println(
                new TemplateResolver("""
                        etwas davor,
                        
                        ${einfacher Platzhalter},
                        ${verschachtelter %{Name}},
                        
                        und nochmal ohne Quotes:
                        
                        %{einfacher Platzhalter},
                        %{verschachtelter %{Name}},
                        
                        etwas danach.
                        """,
                        Map.ofEntries(
                                Map.entry("Name", "placeholder"),
                                Map.entry("einfacher Platzhalter", "simple placeholder"),
                                Map.entry("verschachtelter placeholder", "nested placeholder")
                        )).resolve());

    }
}
