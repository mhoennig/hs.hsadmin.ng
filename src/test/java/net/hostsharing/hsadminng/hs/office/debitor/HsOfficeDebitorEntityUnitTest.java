package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerDetailsEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeDebitorEntityUnitTest {

    @Test
    void toStringContainsPartnerAndContact() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorNumberSuffix((byte)67)
                .partner(HsOfficePartnerEntity.builder()
                        .person(HsOfficePersonEntity.builder()
                                .personType(HsOfficePersonType.LEGAL)
                                .tradeName("some trade name")
                                .build())
                        .details(HsOfficePartnerDetailsEntity.builder().birthName("some birth name").build())
                        .debitorNumberPrefix(12345)
                        .build())
                .billingContact(HsOfficeContactEntity.builder().label("some label").build())
                .defaultPrefix("som")
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("debitor(1234567: LEGAL some trade name: som)");
    }

    @Test
    void toStringWithoutPersonContainsDebitorNumber() {
        final var given = HsOfficeDebitorEntity.builder()
                                .debitorNumberSuffix((byte)67)
                .partner(HsOfficePartnerEntity.builder()
                        .person(null)
                        .details(HsOfficePartnerDetailsEntity.builder().birthName("some birth name").build())
                        .debitorNumberPrefix(12345)
                        .build())
                .billingContact(HsOfficeContactEntity.builder().label("some label").build())
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("debitor(1234567: <person=null>)");
    }

    @Test
    void toShortStringContainsDebitorNumber() {
        final var given = HsOfficeDebitorEntity.builder()
                .partner(HsOfficePartnerEntity.builder()
                        .debitorNumberPrefix(12345)
                        .build())
                .debitorNumberSuffix((byte)67)
                .build();

        final var result = given.toShortString();

        assertThat(result).isEqualTo("1234567");
    }
}
