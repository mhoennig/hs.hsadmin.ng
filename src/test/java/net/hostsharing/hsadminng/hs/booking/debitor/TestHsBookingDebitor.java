package net.hostsharing.hsadminng.hs.booking.debitor;

import lombok.experimental.UtilityClass;


@UtilityClass
public class TestHsBookingDebitor {

    public static final HsBookingDebitorEntity TEST_BOOKING_DEBITOR = HsBookingDebitorEntity.builder()
            .debitorNumber(1234500)
            .defaultPrefix("abc")
            .build();
}
