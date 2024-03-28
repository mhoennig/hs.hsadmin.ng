package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;

public class TestHsOfficePartner {

    public static final HsOfficePartnerEntity TEST_PARTNER = hsOfficePartnerWithLegalPerson("Test Ltd.");

    static public HsOfficePartnerEntity hsOfficePartnerWithLegalPerson(final String tradeName) {
        return HsOfficePartnerEntity.builder()
                .partnerNumber(10001)
                .partnerRel(
                        HsOfficeRelationEntity.builder()
                                .holder(HsOfficePersonEntity.builder()
                                        .personType(LEGAL_PERSON)
                                        .tradeName("Hostsharing eG")
                                        .build())
                                .type(HsOfficeRelationType.PARTNER)
                                .holder(HsOfficePersonEntity.builder()
                                        .personType(LEGAL_PERSON)
                                        .tradeName(tradeName)
                                        .build())
                                .contact(HsOfficeContactEntity.builder()
                                        .label(tradeName)
                                        .build())
                                .build()
                )
                .build();
    }
}
