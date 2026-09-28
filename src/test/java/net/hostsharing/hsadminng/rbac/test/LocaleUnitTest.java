package net.hostsharing.hsadminng.rbac.test;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class LocaleUnitTest {

    @Test
    void testsAreUsingEnUsUtf8Locale() {
        assertThat(Locale.getDefault()).isEqualTo(Locale.of("en", "US"));
        assertThat(new BigDecimal("64.00").toString()).isEqualTo("64.00");
    }
}
