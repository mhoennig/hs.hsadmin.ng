package net.hostsharing.hsadminng.hs.office.partner;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePartnerDetailsEntityTest {

    final HsOfficePartnerDetailsEntity given = HsOfficePartnerDetailsEntity.builder()
            .registrationOffice("Hamburg")
            .registrationNumber("12345")
            .birthday(LocalDate.parse("2002-01-15"))
            .birthName("Melly Miller")
            .dateOfDeath(LocalDate.parse("2081-12-21"))
            .build();

    @Test
    void toStringContainsAllProperties() {

        final var result = given.toString();

        assertThat(result).isEqualTo("partnerDetails(Hamburg, 12345, 2002-01-15, 2002-01-15, 2081-12-21)");
    }

    @Test
    void toShortStringContainsFirstNonNullValue() {

        assertThat(given.toShortString()).isEqualTo("12345");

        assertThat(HsOfficePartnerDetailsEntity.builder()
                .birthName("Melly Miller")
                .birthday(LocalDate.parse("2002-01-15"))
                .dateOfDeath(LocalDate.parse("2081-12-21"))
                .build().toShortString()).isEqualTo("Melly Miller");

        assertThat(HsOfficePartnerDetailsEntity.builder()
                .birthday(LocalDate.parse("2002-01-15"))
                .dateOfDeath(LocalDate.parse("2081-12-21"))
                .build().toShortString()).isEqualTo("2002-01-15");

        assertThat(HsOfficePartnerDetailsEntity.builder()
                .dateOfDeath(LocalDate.parse("2081-12-21"))
                .build().toShortString()).isEqualTo("2081-12-21");


        assertThat(HsOfficePartnerDetailsEntity.builder()
                .build().toShortString()).isEqualTo("<empty details>");
    }
}
