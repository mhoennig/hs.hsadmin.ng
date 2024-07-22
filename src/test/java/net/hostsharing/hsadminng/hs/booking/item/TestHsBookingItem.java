package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.TEST_PROJECT;

@UtilityClass
public class TestHsBookingItem {

    public static final HsBookingItemEntity TEST_CLOUD_SERVER_BOOKING_ITEM = HsBookingItemEntity.builder()
            .project(TEST_PROJECT)
            .type(HsBookingItemType.CLOUD_SERVER)
            .caption("test cloud server booking item")
            .resources(Map.ofEntries(
                    entry("CPU", 2),
                    entry("RAM", 4),
                    entry("SSD", 50),
                    entry("Traffic", 250)
            ))
            .validity(Range.closedInfinite(LocalDate.of(2020, 1, 15)))
            .build();

    public static final HsBookingItemEntity TEST_MANAGED_SERVER_BOOKING_ITEM = HsBookingItemEntity.builder()
            .project(TEST_PROJECT)
            .type(HsBookingItemType.MANAGED_SERVER)
            .caption("test project booking item")
            .resources(Map.ofEntries(
                    entry("CPU", 2),
                    entry("RAM", 4),
                    entry("SSD", 50),
                    entry("Traffic", 250)
            ))
            .validity(Range.closedInfinite(LocalDate.of(2020, 1, 15)))
            .build();

    public static final HsBookingItemEntity TEST_MANAGED_WEBSPACE_BOOKING_ITEM = HsBookingItemEntity.builder()
            .parentItem(TEST_MANAGED_SERVER_BOOKING_ITEM)
            .type(HsBookingItemType.MANAGED_WEBSPACE)
            .caption("test managed webspace item")
            .resources(Map.ofEntries(
                    entry("SSD", 50),
                    entry("Traffic", 250)
            ))
            .validity(Range.closedInfinite(LocalDate.of(2020, 1, 15)))
            .build();

}
