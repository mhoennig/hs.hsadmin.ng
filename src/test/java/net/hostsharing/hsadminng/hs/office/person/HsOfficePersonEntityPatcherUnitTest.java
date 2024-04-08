package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.test.PatchUnitTestBase;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonTypeResource;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class HsOfficePersonEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficePersonPatchResource,
        HsOfficePersonEntity
        > {

    private static final UUID INITIAL_PERSON_UUID = UUID.randomUUID();

    @Override
    protected HsOfficePersonEntity newInitialEntity() {
        final var entity = new HsOfficePersonEntity();
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
    protected HsOfficePersonEntityPatcher createPatcher(final HsOfficePersonEntity entity) {
        return new HsOfficePersonEntityPatcher(entity);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new SimpleProperty<>(
                        "personType",
                        HsOfficePersonPatchResource::setPersonType,
                        HsOfficePersonTypeResource.INCORPORATED_FIRM,
                        HsOfficePersonEntity::setPersonType,
                        HsOfficePersonType.INCORPORATED_FIRM)
                        .notNullable(),
                new JsonNullableProperty<>(
                        "tradeName",
                        HsOfficePersonPatchResource::setTradeName,
                        "patched trade name",
                        HsOfficePersonEntity::setTradeName),
                new JsonNullableProperty<>(
                        "title",
                        HsOfficePersonPatchResource::setTitle,
                        "Dr. Patch.",
                        HsOfficePersonEntity::setTitle),
                new JsonNullableProperty<>(
                        "salutation",
                        HsOfficePersonPatchResource::setSalutation,
                        "Hallo Ini",
                        HsOfficePersonEntity::setSalutation),
                new JsonNullableProperty<>(
                        "familyName",
                        HsOfficePersonPatchResource::setFamilyName,
                        "patched family name",
                        HsOfficePersonEntity::setFamilyName),
                new JsonNullableProperty<>(
                        "patched given name",
                        HsOfficePersonPatchResource::setGivenName,
                        "patched given name",
                        HsOfficePersonEntity::setGivenName)
        );
    }
}
