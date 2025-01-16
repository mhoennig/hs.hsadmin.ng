package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.experimental.UtilityClass;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;

import static net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealTestEntity.TEST_REAL_CONTACT;
import static net.hostsharing.hsadminng.hs.office.partner.HsOfficeTestRealPartner.TEST_PARTNER;

@UtilityClass
public class TestHsOfficeDebitor {

    public String DEFAULT_DEBITOR_SUFFIX = "00";

    public static final HsOfficeDebitorEntity TEST_DEBITOR = HsOfficeDebitorEntity.builder()
            .debitorNumberSuffix(DEFAULT_DEBITOR_SUFFIX)
            .debitorRel(HsOfficeRelationRealEntity.builder()
                    .holder(HsOfficePersonRealEntity.builder().build())
                    .anchor(HsOfficePersonRealEntity.builder().build())
                    .contact(TEST_REAL_CONTACT)
                    .build())
            .partner(TEST_PARTNER)
            .defaultPrefix("abc")
            .build();
}
