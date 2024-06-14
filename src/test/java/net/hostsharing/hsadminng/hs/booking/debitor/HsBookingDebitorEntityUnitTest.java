package net.hostsharing.hsadminng.hs.booking.debitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsBookingDebitorEntityUnitTest {

    @Test
    void toStringContainsDebitorNumberAndDefaultPrefix() {
        final var given = HsBookingDebitorEntity.builder()
                .debitorNumber(1234567)
                .defaultPrefix("som")
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("booking-debitor(D-1234567: som)");
    }

    @Test
    void toShortStringContainsDefaultPrefix() {
        final var given = HsBookingDebitorEntity.builder()
                .debitorNumber(1234567)
                .defaultPrefix("som")
                .build();

        final var result = given.toShortString();

        assertThat(result).isEqualTo("D-1234567");
    }

}
