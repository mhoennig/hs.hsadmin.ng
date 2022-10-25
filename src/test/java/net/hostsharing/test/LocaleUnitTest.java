package net.hostsharing.test;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class LocaleUnitTest {

    @Test
    void testsAreUsingEnUsUtf8Locale() {
        assertThat(Locale.getDefault()).isEqualTo(new Locale("en", "US"));
        assertThat(new BigDecimal("64.00").toString()).isEqualTo("64.00");
    }
}
