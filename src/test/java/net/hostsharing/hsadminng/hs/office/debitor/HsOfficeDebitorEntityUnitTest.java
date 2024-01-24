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
                                .personType(HsOfficePersonType.LEGAL_PERSON)
                                .tradeName("some trade name")
                                .build())
                        .details(HsOfficePartnerDetailsEntity.builder().birthName("some birth name").build())
                        .partnerNumber(12345)
                        .build())
                .billingContact(HsOfficeContactEntity.builder().label("some label").build())
                .defaultPrefix("som")
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("debitor(D-1234567: LP some trade name: som)");
    }

    @Test
    void toStringWithoutPersonContainsDebitorNumber() {
        final var given = HsOfficeDebitorEntity.builder()
                                .debitorNumberSuffix((byte)67)
                .partner(HsOfficePartnerEntity.builder()
                        .person(null)
                        .details(HsOfficePartnerDetailsEntity.builder().birthName("some birth name").build())
                        .partnerNumber(12345)
                        .build())
                .billingContact(HsOfficeContactEntity.builder().label("some label").build())
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("debitor(D-1234567: <person=null>)");
    }

    @Test
    void toShortStringContainsDebitorNumber() {
        final var given = HsOfficeDebitorEntity.builder()
                .partner(HsOfficePartnerEntity.builder()
                        .partnerNumber(12345)
                        .build())
                .debitorNumberSuffix((byte)67)
                .build();

        final var result = given.toShortString();

        assertThat(result).isEqualTo("D-1234567");
    }

    @Test
    void getDebitorNumberWithPartnerNumberAndDebitorNumberSuffix() {
        final var given = HsOfficeDebitorEntity.builder()
                .partner(HsOfficePartnerEntity.builder()
                        .partnerNumber(12345)
                        .build())
                .debitorNumberSuffix((byte)67)
                .build();

        final var result = given.getDebitorNumber();

        assertThat(result).isEqualTo(1234567);
    }

    @Test
    void getDebitorNumberWithoutPartnerReturnsNull() {
        final var given = HsOfficeDebitorEntity.builder()
                .partner(null)
                .debitorNumberSuffix((byte)67)
                .build();

        final var result = given.getDebitorNumber();

        assertThat(result).isNull();
    }

    @Test
    void getDebitorNumberWithoutPartnerNumberReturnsNull() {
        final var given = HsOfficeDebitorEntity.builder()
                .partner(HsOfficePartnerEntity.builder()
                        .partnerNumber(null)
                        .build())
                .debitorNumberSuffix((byte)67)
                .build();

        final var result = given.getDebitorNumber();

        assertThat(result).isNull();
    }

    @Test
    void getDebitorNumberWithoutDebitorNumberSuffixReturnsNull() {
        final var given = HsOfficeDebitorEntity.builder()
                .partner(HsOfficePartnerEntity.builder()
                        .partnerNumber(12345)
                        .build())
                .debitorNumberSuffix(null)
                .build();

        final var result = given.getDebitorNumber();

        assertThat(result).isNull();
    }
}
