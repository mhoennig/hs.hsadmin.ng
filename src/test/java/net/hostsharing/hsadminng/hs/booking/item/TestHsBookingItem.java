package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.PROJECT_TEST_ENTITY;

@UtilityClass
public class TestHsBookingItem {

    public static final HsBookingItemRealEntity CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY = HsBookingItemRealEntity.builder()
            .project(PROJECT_TEST_ENTITY)
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

    public static final HsBookingItemRealEntity MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY = HsBookingItemRealEntity.builder()
            .project(PROJECT_TEST_ENTITY)
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

    public static final HsBookingItemRealEntity MANAGED_WEBSPACE_BOOKING_ITEM_REAL_ENTITY = HsBookingItemRealEntity.builder()
            .parentItem(MANAGED_SERVER_BOOKING_ITEM_REAL_ENTITY)
            .type(HsBookingItemType.MANAGED_WEBSPACE)
            .caption("test managed webspace item")
            .resources(Map.ofEntries(
                    entry("SSD", 50),
                    entry("Traffic", 250)
            ))
            .validity(Range.closedInfinite(LocalDate.of(2020, 1, 15)))
            .build();

}
