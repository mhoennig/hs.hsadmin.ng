package net.hostsharing.hsadminng.hs.booking.item;

import lombok.val;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.PROJECT_TEST_ENTITY;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static org.assertj.core.api.Assertions.assertThat;

class HsBookingItemEntityUnitTest {
    public static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-01-01");
    public static final LocalDate GIVEN_VALID_TO = LocalDate.parse("2030-12-31");

    private MockedStatic<LocalDate> localDateMockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);

    final HsBookingItem givenBookingItem = HsBookingItemRbacEntity.builder()
            .project(PROJECT_TEST_ENTITY)
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
        val fakedToday = LocalDate.of(2024, Month.MAY, 1);
        localDateMockedStatic.when(LocalDate::now).thenReturn(fakedToday);

        // when
        val newBookingItem = HsBookingItemRbacEntity.builder().build();

        // then
        assertThat(newBookingItem.getValidity().toString()).isEqualTo("Range{lower=2024-05-01, upper=null, mask=82, clazz=class java.time.LocalDate}");
    }

    @Test
    void toStringContainsAllPropertiesAndResourcesSortedByKey() {
        val result = givenBookingItem.toString();

        assertThat(result).isEqualToIgnoringWhitespace("HsBookingItem(CLOUD_SERVER, some caption, D-1234500:test project, [2020-01-01,2031-01-01), { \"CPU\": 2, \"HDD-storage\": 2048, \"SSD-storage\": 512 })");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberAndCaption() {
        val result = givenBookingItem.toShortString();

        assertThat(result).isEqualTo("D-1234500:test project:some caption");
    }

    @Test
    void toShortStringFallsBackIfNoRelatedProjectIsAvailable() {
        val result = HsBookingItemRbacEntity.builder()
                .caption("technical item")
                .build()
                .toShortString();

        assertThat(result).isEqualTo("D-???????-?:technical item");
    }

    @Test
    void getContextValueFallsBackToParentResources() {
        val givenParentItem = HsBookingItemRealEntity.builder()
                .resources(Map.ofEntries(entry("CPU", 4)))
                .build();
        val givenChildItem = HsBookingItemRbacEntity.builder()
                .parentItem(givenParentItem)
                .resources(Map.ofEntries(entry("SSD", 25)))
                .build();

        assertThat(givenChildItem.getContextValue("SSD")).isEqualTo(25);
        assertThat(givenChildItem.getContextValue("CPU")).isEqualTo(4);
        assertThat(givenChildItem.getContextValue("RAM")).isNull();
    }

    @Test
    void markAsLoadedSetsLoadedFlag() {
        val givenItem = HsBookingItemRbacEntity.builder().build();

        givenItem.markAsLoaded();

        assertThat(givenItem.isLoaded()).isTrue();
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
