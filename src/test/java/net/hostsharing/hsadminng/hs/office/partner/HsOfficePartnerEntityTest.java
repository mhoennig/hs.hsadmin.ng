package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePartnerEntityTest {

    @Test
    void toStringContainsPersonAndContact() {
        final var given = HsOfficePartnerEntity.builder()
                .person(HsOfficePersonEntity.builder().tradeName("some trade name").build())
                .contact(HsOfficeContactEntity.builder().label("some label").build())
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("partner(some trade name: some label)");
    }

    @Test
    void toShortStringContainsPersonAndContact() {
        final var given = HsOfficePartnerEntity.builder()
                .person(HsOfficePersonEntity.builder().tradeName("some trade name").build())
                .contact(HsOfficeContactEntity.builder().label("some label").build())
                .build();

        final var result = given.toShortString();

        assertThat(result).isEqualTo("some trade name");
    }
}
