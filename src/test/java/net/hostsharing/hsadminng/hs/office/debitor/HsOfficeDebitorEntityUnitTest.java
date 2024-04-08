package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeDebitorEntityUnitTest {

    private HsOfficeRelationEntity givenDebitorRel = HsOfficeRelationEntity.builder()
            .anchor(HsOfficePersonEntity.builder()
                    .personType(HsOfficePersonType.LEGAL_PERSON)
                    .tradeName("some partner trade name")
                    .build())
            .holder(HsOfficePersonEntity.builder()
                    .personType(HsOfficePersonType.LEGAL_PERSON)
                    .tradeName("some billing trade name")
                    .build())
            .contact(HsOfficeContactEntity.builder().label("some label").build())
            .build();

    @Test
    void toStringContainsPartnerAndContact() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorNumberSuffix("67")
                .debitorRel(givenDebitorRel)
                .defaultPrefix("som")
                .partner(HsOfficePartnerEntity.builder()
                        .partnerNumber(12345)
                        .build())
                .build();

        final var result = given.toString();

        assertThat(result).isEqualTo("debitor(D-1234567: rel(anchor='LP some partner trade name', holder='LP some billing trade name'), som)");
    }

    @Test
    void toShortStringContainsDebitorNumber() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorRel(givenDebitorRel)
                .debitorNumberSuffix("67")
                .partner(HsOfficePartnerEntity.builder()
                        .partnerNumber(12345)
                        .build())
                .build();

        final var result = given.toShortString();

        assertThat(result).isEqualTo("D-1234567");
    }

    @Test
    void getDebitorNumberWithPartnerNumberAndDebitorNumberSuffix() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorRel(givenDebitorRel)
                .debitorNumberSuffix("67")
                .partner(HsOfficePartnerEntity.builder()
                        .partnerNumber(12345)
                        .build())
                .build();

        final var result = given.getDebitorNumber();

        assertThat(result).isEqualTo(1234567);
    }

    @Test
    void getDebitorNumberWithoutPartnerReturnsNull() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorRel(givenDebitorRel)
                .debitorNumberSuffix("67")
                .partner(null)
                .build();

        final var result = given.getDebitorNumber();

        assertThat(result).isNull();
    }

    @Test
    void getDebitorNumberWithoutPartnerNumberReturnsNull() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorRel(givenDebitorRel)
                .debitorNumberSuffix("67")
                .partner(HsOfficePartnerEntity.builder().build())
                .build();

        final var result = given.getDebitorNumber();

        assertThat(result).isNull();
    }

    @Test
    void getDebitorNumberWithoutDebitorNumberSuffixReturnsNull() {
        final var given = HsOfficeDebitorEntity.builder()
                .debitorRel(givenDebitorRel)
                .debitorNumberSuffix(null)
                .partner(HsOfficePartnerEntity.builder()
                        .partnerNumber(12345)
                        .build())
                .build();

        final var result = given.getDebitorNumber();

        assertThat(result).isNull();
    }
}
