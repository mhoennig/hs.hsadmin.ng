package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.rbac.test.PatchUnitTestBase;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactPatchResource;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static net.hostsharing.hsadminng.mapper.PatchMap.patchMap;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class HsOfficeContactPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeContactPatchResource,
        HsOfficeContactRbacEntity
        > {

    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();

    private static final  Map<String, String> PATCH_POSTAL_ADDRESS = patchMap(
            entry("name", "Patty Patch"),
            entry("street", "Patchstreet 10"),
            entry("zipcode", null),
            entry("city", "Hamburg")
    );
    private static final  Map<String, String> PATCHED_POSTAL_ADDRESS = patchMap(
            entry("name", "Patty Patch"),
            entry("street", "Patchstreet 10"),
            entry("city", "Hamburg")
    );

    private static final  Map<String, String> PATCH_EMAIL_ADDRESSES = patchMap(
            entry("main", "patched@example.com"),
            entry("paul", null),
            entry("suse", "suse@example.com")
    );
    private static final  Map<String, String> PATCHED_EMAIL_ADDRESSES = patchMap(
            entry("main", "patched@example.com"),
            entry("suse", "suse@example.com"),
            entry("mila", "mila@example.com")
    );

    private static final  Map<String, String> PATCH_PHONE_NUMBERS = patchMap(
            entry("phone_mobile", null),
            entry("phone_private", "+49 40 987654321"),
            entry("fax",  "+49 40 12345-99")
    );
    private static final  Map<String, String> PATCHED_PHONE_NUMBERS = patchMap(
            entry("phone_office", "+49 40 12345-00"),
            entry("phone_private", "+49 40 987654321"),
            entry("fax",  "+49 40 12345-99")
    );

    @Override
    protected HsOfficeContactRbacEntity newInitialEntity() {
        final var entity = new HsOfficeContactRbacEntity();
        entity.setUuid(INITIAL_CONTACT_UUID);
        entity.setCaption("initial caption");
        entity.putPostalAddress(Map.ofEntries(
                entry("name", "Ina Initial"),
                entry("street", "Initialstraße 50"),
                entry("zipcode", "20000"),
                entry("city", "Hamburg")));
        entity.putEmailAddresses(Map.ofEntries(
                entry("main", "initial@example.org"),
                entry("paul", "paul@example.com"),
                entry("mila", "mila@example.com")));
        entity.putPhoneNumbers(Map.ofEntries(
                entry("phone_office", "+49 40 12345-00"),
                entry("phone_mobile", "+49 1555 1234567"),
                entry("fax",  "+49 40 12345-90")));
        return entity;
    }

    @Override
    protected HsOfficeContactPatchResource newPatchResource() {
        return new HsOfficeContactPatchResource();
    }

    @Override
    protected HsOfficeContactEntityPatcher createPatcher(final HsOfficeContactRbacEntity entity) {
        return new HsOfficeContactEntityPatcher(entity);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "caption",
                        HsOfficeContactPatchResource::setCaption,
                        "patched caption",
                        HsOfficeContactRbacEntity::setCaption),
                new SimpleProperty<>(
                        "postalAddress",
                        HsOfficeContactPatchResource::setPostalAddress,
                        PATCH_POSTAL_ADDRESS,
                        HsOfficeContactRbacEntity::putPostalAddress,
                        PATCHED_POSTAL_ADDRESS)
                        .notNullable(),
                new SimpleProperty<>(
                        "emailAddresses",
                        HsOfficeContactPatchResource::setEmailAddresses,
                        PATCH_EMAIL_ADDRESSES,
                        HsOfficeContactRbacEntity::putEmailAddresses,
                        PATCHED_EMAIL_ADDRESSES)
                        .notNullable(),
                new SimpleProperty<>(
                        "phoneNumbers",
                        HsOfficeContactPatchResource::setPhoneNumbers,
                        PATCH_PHONE_NUMBERS,
                        HsOfficeContactRbacEntity::putPhoneNumbers,
                        PATCHED_PHONE_NUMBERS)
                        .notNullable()
        );
    }
}
