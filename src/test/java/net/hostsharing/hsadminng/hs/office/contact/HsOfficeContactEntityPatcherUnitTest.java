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
class HsOfficeContactEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeContactPatchResource,
        HsOfficeContactEntity
        > {

    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();
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
    protected HsOfficeContactEntity newInitialEntity() {
        final var entity = new HsOfficeContactEntity();
        entity.setUuid(INITIAL_CONTACT_UUID);
        entity.setCaption("initial caption");
        entity.putEmailAddresses(Map.ofEntries(
                entry("main", "initial@example.org"),
                entry("paul", "paul@example.com"),
                entry("mila", "mila@example.com")));
        entity.putPhoneNumbers(Map.ofEntries(
                entry("phone_office", "+49 40 12345-00"),
                entry("phone_mobile", "+49 1555 1234567"),
                entry("fax",  "+49 40 12345-90")));
        entity.setPostalAddress("Initialstraße 50\n20000 Hamburg");
        return entity;
    }

    @Override
    protected HsOfficeContactPatchResource newPatchResource() {
        return new HsOfficeContactPatchResource();
    }

    @Override
    protected HsOfficeContactEntityPatcher createPatcher(final HsOfficeContactEntity entity) {
        return new HsOfficeContactEntityPatcher(entity);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "caption",
                        HsOfficeContactPatchResource::setCaption,
                        "patched caption",
                        HsOfficeContactEntity::setCaption),
                new SimpleProperty<>(
                        "resources",
                        HsOfficeContactPatchResource::setEmailAddresses,
                        PATCH_EMAIL_ADDRESSES,
                        HsOfficeContactEntity::putEmailAddresses,
                        PATCHED_EMAIL_ADDRESSES)
                        .notNullable(),
                new SimpleProperty<>(
                        "resources",
                        HsOfficeContactPatchResource::setPhoneNumbers,
                        PATCH_PHONE_NUMBERS,
                        HsOfficeContactEntity::putPhoneNumbers,
                        PATCHED_PHONE_NUMBERS)
                        .notNullable(),
                new JsonNullableProperty<>(
                        "patched given name",
                        HsOfficeContactPatchResource::setPostalAddress,
                        "patched given name",
                        HsOfficeContactEntity::setPostalAddress)
        );
    }
}
