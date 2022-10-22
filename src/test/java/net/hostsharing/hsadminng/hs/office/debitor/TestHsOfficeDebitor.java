package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.experimental.UtilityClass;

import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.contact.TestHsOfficeContact.TEST_CONTACT;
import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.TEST_PARTNER;

@UtilityClass
public class TestHsOfficeDebitor {

    public static final HsOfficeDebitorEntity TEST_DEBITOR = HsOfficeDebitorEntity.builder()
            .uuid(UUID.randomUUID())
            .debitorNumber(10001)
            .partner(TEST_PARTNER)
            .billingContact(TEST_CONTACT)
            .build();
}
