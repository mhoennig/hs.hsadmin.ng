package net.hostsharing.hsadminng.hs.booking.project;

import lombok.experimental.UtilityClass;

import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.TEST_DEBITOR;

@UtilityClass
public class TestHsBookingProject {


    public static final HsBookingProjectEntity TEST_PROJECT = HsBookingProjectEntity.builder()
            .debitor(TEST_DEBITOR)
            .caption("test project")
            .build();
}
