package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.experimental.UtilityClass;


import static net.hostsharing.hsadminng.hs.office.contact.TestHsOfficeContact.TEST_CONTACT;
import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.TEST_PARTNER;

@UtilityClass
public class TestHsOfficeDebitor {

    public byte DEFAULT_DEBITOR_SUFFIX = 0;

    public static final HsOfficeDebitorEntity TEST_DEBITOR = HsOfficeDebitorEntity.builder()
            .debitorNumberSuffix(DEFAULT_DEBITOR_SUFFIX)
            .partner(TEST_PARTNER)
            .billingContact(TEST_CONTACT)
            .build();
}
