package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;

import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL;

public class TestHsOfficePartner {

    public static final HsOfficePartnerEntity testLtd = HsOfficePartnerWithLegalPerson("Test Ltd.");

    static public HsOfficePartnerEntity HsOfficePartnerWithLegalPerson(final String tradeName) {
        return HsOfficePartnerEntity.builder()
                .uuid(UUID.randomUUID())
                .person(HsOfficePersonEntity.builder()
                        .personType(LEGAL)
                        .tradeName(tradeName)
                        .build())
                .contact(HsOfficeContactEntity.builder()
                        .label(tradeName)
                        .build())
                .build();
    }
}
