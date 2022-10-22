package net.hostsharing.hsadminng.mapper;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static org.assertj.core.api.Assertions.assertThat;

class PostgresDateRangeUnitTest {

    @Test
    void createsInfiniteRange() {
        final var actual = toPostgresDateRange(null, null);
        assertThat(actual.toString()).isEqualTo("Range{lower=null, upper=null, mask=116, clazz=class java.time.LocalDate}");
    }

    @Test
    void createsClosedInfiniteRange() {
        final var actual = toPostgresDateRange(LocalDate.parse("2020-10-31"), null);
        assertThat(actual.toString()).isEqualTo("Range{lower=2020-10-31, upper=null, mask=82, clazz=class java.time.LocalDate}");
    }

    @Test
    void createsInfiniteOpenRange() {
        final var actual = toPostgresDateRange(null, LocalDate.parse("2020-10-31"));
        assertThat(actual.toString()).isEqualTo("Range{lower=null, upper=2020-11-01, mask=52, clazz=class java.time.LocalDate}");
    }

    @Test
    void createsClosedOpenRange() {
        final var actual = toPostgresDateRange(LocalDate.parse("2020-10-31"), LocalDate.parse("2020-12-31"));
        assertThat(actual.toString()).isEqualTo(
                "Range{lower=2020-10-31, upper=2021-01-01, mask=18, clazz=class java.time.LocalDate}");
    }

}
