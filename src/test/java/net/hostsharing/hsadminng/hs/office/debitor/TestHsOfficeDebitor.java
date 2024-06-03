package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.experimental.UtilityClass;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;

import static net.hostsharing.hsadminng.hs.office.contact.TestHsOfficeContact.TEST_CONTACT;
import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.TEST_PARTNER;

@UtilityClass
public class TestHsOfficeDebitor {

    public String DEFAULT_DEBITOR_SUFFIX = "00";

    public static final HsOfficeDebitorEntity TEST_DEBITOR = HsOfficeDebitorEntity.builder()
            .debitorNumberSuffix(DEFAULT_DEBITOR_SUFFIX)
            .debitorRel(HsOfficeRelationEntity.builder()
                    .holder(HsOfficePersonEntity.builder().build())
                    .anchor(HsOfficePersonEntity.builder().build())
                    .contact(TEST_CONTACT)
                    .build())
            .partner(TEST_PARTNER)
            .defaultPrefix("abc")
            .build();
}
