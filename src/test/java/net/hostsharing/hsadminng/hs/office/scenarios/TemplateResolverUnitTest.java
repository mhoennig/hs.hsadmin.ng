package net.hostsharing.hsadminng.hs.office.scenarios;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static net.hostsharing.hsadminng.hs.office.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;

class TemplateResolverUnitTest {

    @Test
    void resolveTemplate() {
        final var resolved = new TemplateResolver("""
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
                Map.ofEntries(
                        Map.entry("name", "placeholder"),
                        Map.entry("boolean", true),
                        Map.entry("numeric", 42),
                        Map.entry("simple placeholder", "einfach"),
                        Map.entry("nested placeholder", "verschachtelt"),
                        Map.entry("with-special-chars", "3&3 AG")
                )).resolve(DROP_COMMENTS);

        assertThat(resolved).isEqualTo("""
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
}
