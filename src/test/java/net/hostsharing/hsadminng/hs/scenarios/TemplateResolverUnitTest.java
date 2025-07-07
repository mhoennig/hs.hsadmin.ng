package net.hostsharing.hsadminng.hs.scenarios;

import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;

class TemplateResolverUnitTest {

    @Test
    void resolveTemplate() {
        final var resolved = new TemplateResolver("""
                    JSON arrays:
                    - arrayWithMixedValues: @{arrayWithMixedValues}
                    - arrayWithObjects: @{arrayWithObjects}
                    - emptyArray: @{emptyArray}
                    - nullArray: @{nullArray}
                    
                    with optional JSON quotes:

                    ${boolean},
                    ${numeric},
                    ${simple placeholder},
                    ${nested %{name}},
                    ${with-special-chars}
                    
                    and without quotes:

                    %{boolean},
                    %{numeric},
                    %{simple placeholder},
                    %{nested %{name}},
                    %{with-special-chars}

                    and uri-encoded:

                    &{boolean},
                    &{numeric},
                    &{simple placeholder},
                    &{nested %{name}},
                    &{with-special-chars}
                    """,
                orderedMapOfElementsWithNullValues(
                    entry("arrayWithMixedValues", new Object[] { "some string", true, 1234, "another string" }),
                    entry("arrayWithObjects", new Object[] {
                            orderedMapOfElementsWithNullValues(
                                    Map.entry("name", "some name"),
                                    Map.entry("number", 12345)
                            ),
                            orderedMapOfElementsWithNullValues(
                                    Map.entry("name", "another name"),
                                    Map.entry("number", 98765)
                            )
                    }),
                    entry("emptyArray", new Object[] {}),
                    entry("nullArray", null),
                    entry("name", "placeholder"),
                    entry("boolean", true),
                    entry("numeric", 42),
                    entry("simple placeholder", "einfach"),
                    entry("nested placeholder", "verschachtelt"),
                    entry("with-special-chars", "3&3 AG")
                )).resolve(DROP_COMMENTS);

        assertThat(resolved).isEqualTo("""
                JSON arrays:
                - arrayWithMixedValues: ["some string", true, 1234, "another string"]
                - arrayWithObjects: [{"name": "some name", "number": 12345}, {"name": "another name", "number": 98765}]
                - emptyArray: []
                
                with optional JSON quotes:

                true,
                42,
                "einfach",
                "verschachtelt",
                "3&3 AG"

                and without quotes:

                true,
                42,
                einfach,
                verschachtelt,
                3&3 AG

                and uri-encoded:

                true,
                42,
                einfach,
                verschachtelt,
                3%263+AG
                """.trim());
    }

    @SafeVarargs
    private Map<String, Object> orderedMapOfElementsWithNullValues(
            final Map.Entry<String, Object>... entries) {
        final var map = new LinkedHashMap<String, Object>();
        if (entries != null) {
            Arrays.stream(entries)
                    .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        }
        return map;
    }

    private static AbstractMap.SimpleEntry<String, Object> entry(String key, Object value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

}
