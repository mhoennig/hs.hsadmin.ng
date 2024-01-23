package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePartnerEntityUnitTest {

    @Test
    void toStringContainsPersonAndContact() {
        final var given = HsOfficePartnerEntity.builder()
                .person(HsOfficePersonEntity.builder()
                        .personType(HsOfficePersonType.LEGAL)
                        .tradeName("some trade name")
                        .build())
                .contact(HsOfficeContactEntity.builder().label("some label").build())
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("partner(LEGAL some trade name: some label)");
    }

    @Test
    void toShortStringContainsPersonAndContact() {
        final var given = HsOfficePartnerEntity.builder()
                .person(HsOfficePersonEntity.builder()
                        .personType(HsOfficePersonType.LEGAL)
                        .tradeName("some trade name")
                        .build())
                .contact(HsOfficeContactEntity.builder().label("some label").build())
                .build();

        final var result = given.toShortString();

        assertThat(result).isEqualTo("LEGAL some trade name");
    }
}
