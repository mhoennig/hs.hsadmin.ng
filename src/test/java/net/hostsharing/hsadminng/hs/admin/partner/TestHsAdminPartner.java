package net.hostsharing.hsadminng.hs.admin.partner;

import net.hostsharing.hsadminng.hs.admin.contact.HsAdminContactEntity;
import net.hostsharing.hsadminng.hs.admin.partner.HsAdminPartnerEntity;
import net.hostsharing.hsadminng.hs.admin.person.HsAdminPersonEntity;

import java.util.UUID;

import static net.hostsharing.hsadminng.hs.admin.person.HsAdminPersonEntity.PersonType.LEGAL;

public class TestHsAdminPartner {

    public static final HsAdminPartnerEntity testLtd = hsAdminPartnerWithLegalPerson("Test Ltd.");

    static public HsAdminPartnerEntity hsAdminPartnerWithLegalPerson(final String tradeName) {
        return HsAdminPartnerEntity.builder()
                .uuid(UUID.randomUUID())
                .person(HsAdminPersonEntity.builder()
                        .type(LEGAL)
                        .tradeName(tradeName)
                        .build())
                .contact(HsAdminContactEntity.builder()
                        .label(tradeName)
                        .build())
                .build();
    }
}
