package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePartnerEntityUnitTest {

    private final HsOfficePartnerEntity givenPartner = HsOfficePartnerEntity.builder()
            .partnerNumber(12345)
            .partnerRel(HsOfficeRelationEntity.builder()
                    .anchor(HsOfficePersonEntity.builder()
                            .personType(HsOfficePersonType.LEGAL_PERSON)
                            .tradeName("Hostsharing eG")
                            .build())
                    .type(HsOfficeRelationType.PARTNER)
                    .holder(HsOfficePersonEntity.builder()
                            .personType(HsOfficePersonType.LEGAL_PERSON)
                            .tradeName("some trade name")
                            .build())
                    .contact(HsOfficeContactEntity.builder().label("some label").build())
                    .build())
            .build();

    @Test
    void toStringContainsPartnerNumberPersonAndContact() {
        final var result = givenPartner.toString();
        assertThat(result).isEqualTo("partner(P-12345: LP some trade name, some label)");
    }

    @Test
    void toShortStringContainsPartnerNumber() {
        final var result = givenPartner.toShortString();
        assertThat(result).isEqualTo("P-12345");
    }
}
