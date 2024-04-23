package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.TEST_DEBITOR;

@UtilityClass
public class TestHsBookingItem {

    public static final HsBookingItemEntity TEST_BOOKING_ITEM = HsBookingItemEntity.builder()
            .debitor(TEST_DEBITOR)
            .caption("test booking item")
            .resources(Map.ofEntries(
                    entry("someThing", 1),
                    entry("anotherThing", "blue")
            ))
            .validity(Range.closedInfinite(LocalDate.of(2020, 1, 15)))
            .build();
}
