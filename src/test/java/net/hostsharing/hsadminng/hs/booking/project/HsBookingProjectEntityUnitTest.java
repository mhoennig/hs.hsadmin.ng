package net.hostsharing.hsadminng.hs.booking.project;

import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.hs.booking.debitor.TestHsBookingDebitor.TEST_BOOKING_DEBITOR;
import static org.assertj.core.api.Assertions.assertThat;

class HsBookingProjectEntityUnitTest {
    final HsBookingProjectEntity givenBookingProject = HsBookingProjectEntity.builder()
            .debitor(TEST_BOOKING_DEBITOR)
            .caption("some caption")
            .build();

    @Test
    void toStringContainsAllPropertiesAndResourcesSortedByKey() {
        final var result = givenBookingProject.toString();

        assertThat(result).isEqualTo("HsBookingProjectEntity(D-1234500, some caption)");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberAndCaption() {
        final var result = givenBookingProject.toShortString();

        assertThat(result).isEqualTo("D-1234500:some caption");
    }
}
