package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeDebitorEntityTest {

    @Test
    void toStringContainsPartnerAndContact() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorNumber(123456)
                .partner(HsOfficePartnerEntity.builder()
                        .person(HsOfficePersonEntity.builder()
                                .tradeName("some trade name")
                                .build())
                        .birthName("some birth name")
                        .build())
                .billingContact(HsOfficeContactEntity.builder().label("some label").build())
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("debitor(123456: some trade name)");
    }

    @Test
    void toShortStringContainsPartnerAndContact() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorNumber(123456)
                .build();

        final var result = given.toShortString();

        assertThat(result).isEqualTo("123456");
    }
}
