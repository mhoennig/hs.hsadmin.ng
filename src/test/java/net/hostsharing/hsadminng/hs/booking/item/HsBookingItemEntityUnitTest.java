package net.hostsharing.hsadminng.hs.booking.item;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.TEST_PROJECT;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static org.assertj.core.api.Assertions.assertThat;

class HsBookingItemEntityUnitTest {
    public static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-01-01");
    public static final LocalDate GIVEN_VALID_TO = LocalDate.parse("2030-12-31");

    private MockedStatic<LocalDate> localDateMockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);

    final HsBookingItemEntity givenBookingItem = HsBookingItemEntity.builder()
            .project(TEST_PROJECT)
            .type(HsBookingItemType.CLOUD_SERVER)
            .caption("some caption")
            .resources(Map.ofEntries(
                    entry("CPU", 2),
                    entry("SSD-storage", 512),
                    entry("HDD-storage", 2048)))
            .validity(toPostgresDateRange(GIVEN_VALID_FROM, GIVEN_VALID_TO))
            .build();

    @AfterEach
    void tearDown() {
        localDateMockedStatic.close();
    }

    @Test
    void validityStartsToday() {
        // given
        final var fakedToday = LocalDate.of(2024, Month.MAY, 1);
        localDateMockedStatic.when(LocalDate::now).thenReturn(fakedToday);

        // when
        final var newBookingItem = HsBookingItemEntity.builder().build();

        // then
        assertThat(newBookingItem.getValidity().toString()).isEqualTo("Range{lower=2024-05-01, upper=null, mask=82, clazz=class java.time.LocalDate}");
    }

    @Test
    void toStringContainsAllPropertiesAndResourcesSortedByKey() {
        final var result = givenBookingItem.toString();

        assertThat(result).isEqualToIgnoringWhitespace("HsBookingItemEntity(D-1234500:test project, CLOUD_SERVER, [2020-01-01,2031-01-01), some caption, { \"CPU\": 2, \"HDD-storage\": 2048, \"SSD-storage\": 512 })");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberAndCaption() {
        final var result = givenBookingItem.toShortString();

        assertThat(result).isEqualTo("D-1234500:test project:some caption");
    }

    @Test
    void settingValidFromKeepsValidTo() {
        givenBookingItem.setValidFrom(LocalDate.parse("2023-12-31"));
        assertThat(givenBookingItem.getValidFrom()).isEqualTo(LocalDate.parse("2023-12-31"));
        assertThat(givenBookingItem.getValidTo()).isEqualTo(GIVEN_VALID_TO);

    }

    @Test
    void settingValidToKeepsValidFrom() {
        givenBookingItem.setValidTo(LocalDate.parse("2024-12-31"));
        assertThat(givenBookingItem.getValidFrom()).isEqualTo(GIVEN_VALID_FROM);
        assertThat(givenBookingItem.getValidTo()).isEqualTo(LocalDate.parse("2024-12-31"));
    }

}
