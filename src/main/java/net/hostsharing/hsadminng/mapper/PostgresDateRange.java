package net.hostsharing.hsadminng.mapper;

import com.vladmihalcea.hibernate.type.range.Range;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;

@UtilityClass
public class PostgresDateRange {

    public static Range<LocalDate> toPostgresDateRange(
            final LocalDate validFrom,
            final LocalDate validTo) {
        return validTo != null
                ? Range.closedOpen(validFrom, validTo.plusDays(1))
                : Range.closedInfinite(validFrom);
    }
}
