package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.hsadminng.rbac.test.PatchUnitTestBase;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonTypeResource;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class HsOfficePersonEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficePersonPatchResource,
        HsOfficePersonRbacEntity
        > {

    private static final UUID INITIAL_PERSON_UUID = UUID.randomUUID();

    @Override
    protected HsOfficePersonRbacEntity newInitialEntity() {
        final var entity = new HsOfficePersonRbacEntity();
        entity.setUuid(INITIAL_PERSON_UUID);
        entity.setPersonType(HsOfficePersonType.LEGAL_PERSON);
        entity.setTradeName("initial trade name");
        entity.setTitle("Dr. Init.");
        entity.setSalutation("Herr Initial");
        entity.setFamilyName("initial postal address");
        entity.setGivenName("+01 100 123456789");
        return entity;
    }

    @Override
    protected HsOfficePersonPatchResource newPatchResource() {
        return new HsOfficePersonPatchResource();
    }

    @Override
    protected HsOfficePersonEntityPatcher createPatcher(final HsOfficePersonRbacEntity entity) {
        return new HsOfficePersonEntityPatcher(entity);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new SimpleProperty<>(
                        "personType",
                        HsOfficePersonPatchResource::setPersonType,
                        HsOfficePersonTypeResource.INCORPORATED_FIRM,
                        HsOfficePersonRbacEntity::setPersonType,
                        HsOfficePersonType.INCORPORATED_FIRM)
                        .notNullable(),
                new JsonNullableProperty<>(
                        "tradeName",
                        HsOfficePersonPatchResource::setTradeName,
                        "patched trade name",
                        HsOfficePersonRbacEntity::setTradeName),
                new JsonNullableProperty<>(
                        "title",
                        HsOfficePersonPatchResource::setTitle,
                        "Dr. Patch.",
                        HsOfficePersonRbacEntity::setTitle),
                new JsonNullableProperty<>(
                        "salutation",
                        HsOfficePersonPatchResource::setSalutation,
                        "Hallo Ini",
                        HsOfficePersonRbacEntity::setSalutation),
                new JsonNullableProperty<>(
                        "familyName",
                        HsOfficePersonPatchResource::setFamilyName,
                        "patched family name",
                        HsOfficePersonRbacEntity::setFamilyName),
                new JsonNullableProperty<>(
                        "patched given name",
                        HsOfficePersonPatchResource::setGivenName,
                        "patched given name",
                        HsOfficePersonRbacEntity::setGivenName)
        );
    }
}
