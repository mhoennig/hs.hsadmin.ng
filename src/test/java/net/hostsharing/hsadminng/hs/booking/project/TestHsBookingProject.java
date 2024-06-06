package net.hostsharing.hsadminng.hs.booking.project;

import lombok.experimental.UtilityClass;

import static net.hostsharing.hsadminng.hs.booking.debitor.TestHsBookingDebitor.TEST_BOOKING_DEBITOR;

@UtilityClass
public class TestHsBookingProject {


    public static final HsBookingProjectEntity TEST_PROJECT = HsBookingProjectEntity.builder()
            .debitor(TEST_BOOKING_DEBITOR)
            .caption("test project")
            .build();
}
