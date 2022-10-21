package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.test.PatchUnitTestBase;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactPatchResource;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class HsOfficeContactEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeContactPatchResource,
        HsOfficeContactEntity
        > {

    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();

    @Override
    protected HsOfficeContactEntity newInitialEntity() {
        final var entity = new HsOfficeContactEntity();
        entity.setUuid(INITIAL_CONTACT_UUID);
        entity.setLabel("initial label");
        entity.setEmailAddresses("initial@example.org");
        entity.setPhoneNumbers("initial postal address");
        entity.setPostalAddress("+01 100 123456789");
        return entity;
    }

    @Override
    protected HsOfficeContactPatchResource newPatchResource() {
        return new HsOfficeContactPatchResource();
    }

    @Override
    protected HsOfficeContactEntityPatch createPatcher(final HsOfficeContactEntity entity) {
        return new HsOfficeContactEntityPatch(entity);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "label",
                        HsOfficeContactPatchResource::setLabel,
                        "patched label",
                        HsOfficeContactEntity::setLabel),
                new JsonNullableProperty<>(
                        "emailAddresses",
                        HsOfficeContactPatchResource::setEmailAddresses,
                        "patched trade name",
                        HsOfficeContactEntity::setEmailAddresses),
                new JsonNullableProperty<>(
                        "phoneNumbers",
                        HsOfficeContactPatchResource::setPhoneNumbers,
                        "patched family name",
                        HsOfficeContactEntity::setPhoneNumbers),
                new JsonNullableProperty<>(
                        "patched given name",
                        HsOfficeContactPatchResource::setPostalAddress,
                        "patched given name",
                        HsOfficeContactEntity::setPostalAddress)
        );
    }
}
