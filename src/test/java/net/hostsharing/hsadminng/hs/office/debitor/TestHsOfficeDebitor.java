package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.experimental.UtilityClass;

import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.contact.TestHsOfficeContact.someContact;
import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.testPartner;

@UtilityClass
public class TestHsOfficeDebitor {

    public static final HsOfficeDebitorEntity testDebitor = HsOfficeDebitorEntity.builder()
            .uuid(UUID.randomUUID())
            .debitorNumber(10001)
            .partner(testPartner)
            .billingContact(someContact)
            .build();
}
