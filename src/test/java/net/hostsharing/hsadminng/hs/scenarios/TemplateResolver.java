package net.hostsharing.hsadminng.hs.scenarios;

import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

public class TemplateResolver {

    public enum Resolver {
        DROP_COMMENTS,  // deletes comments ('#{whatever}' -> '')
        KEEP_COMMENTS   // keep comments ('#{whatever}' -> 'whatever')
    }

    enum PlaceholderPrefix {
        RAW('%') {
            @Override
            String convert(final Object value, final Resolver resolver) {
                return value != null ? value.toString() : "";
            }
        },
        JSON_QUOTED('$'){
            @Override
            String convert(final Object value, final Resolver resolver) {
                return jsonQuoted(value);
            }
        },
        JSON_OBJECT('§'){
            @Override
            String convert(final Object value, final Resolver resolver) {
                return jsonObject(value);
            }
        },
        URI_ENCODED('&'){
            @Override
            String convert(final Object value, final Resolver resolver) {
                return value != null ? URLEncoder.encode(value.toString(), StandardCharsets.UTF_8) : "";
            }
        },
        COMMENT('#'){
            @Override
            String convert(final Object value, final Resolver resolver) {
                return resolver == DROP_COMMENTS ? "" : value.toString();
            }
        };

        private final char prefixChar;

        PlaceholderPrefix(final char prefixChar) {
            this.prefixChar = prefixChar;
        }

        static boolean contains(final char givenChar) {
            return Arrays.stream(values()).anyMatch(p -> p.prefixChar == givenChar);
        }

        static PlaceholderPrefix ofPrefixChar(final char givenChar) {
            return Arrays.stream(values()).filter(p -> p.prefixChar == givenChar).findFirst().orElseThrow();
        }

        abstract String convert(final Object value, final Resolver resolver);
    }

    private static final Pattern COMMA_RIGHT_BEFORE_CLOSING_BRACE = Pattern.compile(",(\\s*})", Pattern.MULTILINE);
    private static final String IF_NOT_FOUND_SYMBOL = "???";

    private final String template;
    private final Map<String, Object> properties;
    private final StringBuilder resolved = new StringBuilder();

    private Resolver resolver;
    private int position = 0;

    public TemplateResolver(final String template, final Map<String, Object> properties) {
        this.template = template;
        this.properties = properties;
    }

    String resolve(final Resolver resolver) {
        this.resolver = resolver;
        final var resolved = copy();
        final var withoutDroppedLines = dropLinesWithNullProperties(resolved);
        final var result = removeDanglingCommas(withoutDroppedLines);
        return result;
    }

    private static String removeDanglingCommas(final String withoutDroppedLines) {
        return COMMA_RIGHT_BEFORE_CLOSING_BRACE.matcher(withoutDroppedLines).replaceAll("$1");
    }

    private String dropLinesWithNullProperties(final String text) {
        return Arrays.stream(text.split("\n"))
                .filter(TemplateResolver::keepLine)
                .map(TemplateResolver::keptNullValues)
                .collect(Collectors.joining("\n"));
    }

    private static boolean keepLine(final String line) {
        final var trimmed = line.trim();
        return !trimmed.endsWith("null,") && !trimmed.endsWith("null");
    }

    private static String keptNullValues(final String line) {
        return line.replace(": NULL", ": null");
    }

    private String copy() {
        while (hasMoreChars()) {
            if (PlaceholderPrefix.contains(currentChar()) && nextChar() == '{') {
                startPlaceholder(currentChar());
            } else {
                resolved.append(fetchChar());
            }
        }
        return resolved.toString();
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
            } else if (PlaceholderPrefix.contains (currentChar()) && nextChar() == '{') {
                ++nested;
                placeholder.append(fetchChar());
            } else {
                placeholder.append(fetchChar());
            }
        }
        final var content = new TemplateResolver(placeholder.toString(), properties).resolve(resolver);
        final var value = intro != '#' ? propVal(content) : content;
        resolved.append(
                PlaceholderPrefix.ofPrefixChar(intro).convert(value, resolver)
        );
        skipChar('}');
    }

    private Object propVal(final String nameExpression) {
        if (nameExpression.endsWith(IF_NOT_FOUND_SYMBOL)) {
            final String pureName = nameExpression.substring(0, nameExpression.length() - IF_NOT_FOUND_SYMBOL.length());
            return properties.get(pureName);
        } else if (nameExpression.contains(IF_NOT_FOUND_SYMBOL)) {
            final var parts = StringUtils.split(nameExpression, IF_NOT_FOUND_SYMBOL);
            return Arrays.stream(parts).filter(Objects::nonNull).findFirst().orElseGet(() -> {
                if ( parts[parts.length-1].isEmpty() ) {
                    // => whole expression ends with IF_NOT_FOUND_SYMBOL, thus last null element was optional
                    return null;
                }
                // => last alternative element in expression was null and not optional
                throw new IllegalStateException("Missing required value in property-chain: " + nameExpression);
            });
        } else {
            final var val = properties.get(nameExpression);
            if (val == null) {
                throw new IllegalStateException("Missing required property: " + nameExpression);
            }
            return val;
        }
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

    private static String jsonQuoted(final Object value) {
        return switch (value) {
            case null -> null;
            case Boolean bool -> bool.toString();
            case Number number -> number.toString();
            case String string -> "\"" + string.replace("\n", "\\n") + "\"";
            default -> "\"" + value + "\"";
        };
    }

    private static String jsonObject(final Object value) {
        return switch (value) {
            case null -> null;
            case String string -> "{" + string.replace("\n", " ") + "}";
            default -> throw new IllegalArgumentException("can not format " + value.getClass() + " (" + value + ") as JSON object");
        };
    }
}
