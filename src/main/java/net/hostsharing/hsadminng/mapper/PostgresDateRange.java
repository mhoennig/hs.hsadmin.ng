package net.hostsharing.hsadminng.mapper;

import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;

@UtilityClass
public class PostgresDateRange {

    public static Range<LocalDate> toPostgresDateRange(
            final LocalDate lowerInclusive,
            final LocalDate upperInclusive) {
        return lowerInclusive != null
                ? upperInclusive != null
                    ? Range.closedOpen(lowerInclusive, upperInclusive.plusDays(1))
                    : Range.closedInfinite(lowerInclusive)
                : upperInclusive != null
                    ? Range.infiniteOpen(upperInclusive.plusDays(1))
                    : Range.infinite(LocalDate.class);
    }

    public static LocalDate lowerInclusiveFromPostgresDateRange(
            final Range<LocalDate> range) {
        return range.lower();
    }

    public static LocalDate upperInclusiveFromPostgresDateRange(
        final Range<LocalDate> range) {
        return range.upper() != null ? range.upper().minusDays(1) : null;
    }

}
