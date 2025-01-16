package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;

public class HsOfficeTestRealPartner {

    public static final HsOfficePartnerRealEntity TEST_PARTNER = hsOfficePartnerWithLegalPerson("Test Ltd.");

    static public HsOfficePartnerRealEntity hsOfficePartnerWithLegalPerson(final String tradeName) {
        return HsOfficePartnerRealEntity.builder()
                .partnerNumber(10001)
                .partnerRel(
                        HsOfficeRelationRealEntity.builder()
                                .holder(HsOfficePersonRealEntity.builder()
                                        .personType(LEGAL_PERSON)
                                        .tradeName("Hostsharing eG")
                                        .build())
                                .type(HsOfficeRelationType.PARTNER)
                                .holder(HsOfficePersonRealEntity.builder()
                                        .personType(LEGAL_PERSON)
                                        .tradeName(tradeName)
                                        .build())
                                .contact(HsOfficeContactRealEntity.builder()
                                        .caption(tradeName)
                                        .build())
                                .build()
                )
                .build();
    }
}
