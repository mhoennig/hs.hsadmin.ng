package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactFromResourceConverter;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeContactInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonInsertResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePersonTypeResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

import jakarta.validation.ValidationException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.LEGAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.PARTNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsOfficeRelationPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeRelationPatchResource,
        HsOfficeRelation
        > {

    private static final UUID INITIAL_RELATION_UUID = UUID.randomUUID();
    private static final UUID INITIAL_ANCHOR_UUID = UUID.randomUUID();
    private static final UUID INITIAL_HOLDER_UUID = UUID.randomUUID();
    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();

    private static final UUID PATCHED_HOLDER_UUID = UUID.randomUUID();
    private static HsOfficePersonInsertResource HOLDER_PATCH_RESOURCE = new HsOfficePersonInsertResource();
    {
        {
            HOLDER_PATCH_RESOURCE.setPersonType(HsOfficePersonTypeResource.NATURAL_PERSON);
            HOLDER_PATCH_RESOURCE.setFamilyName("Patched-Holder-Family-Name");
            HOLDER_PATCH_RESOURCE.setGivenName("Patched-Holder-Given-Name");
        }
    };
    private static HsOfficePersonRealEntity PATCHED_HOLDER = HsOfficePersonRealEntity.builder()
            .uuid(PATCHED_HOLDER_UUID)
            .personType(NATURAL_PERSON)
            .familyName("Patched-Holder-Family-Name")
            .givenName("Patched-Holder-Given-Name")
            .build();

    private static final UUID PATCHED_CONTACT_UUID = UUID.randomUUID();
    private static HsOfficeContactInsertResource CONTACT_PATCH_RESOURCE = new HsOfficeContactInsertResource();
    {
        {
            CONTACT_PATCH_RESOURCE.setCaption("Patched-Contact-Caption");
            CONTACT_PATCH_RESOURCE.setEmailAddresses(Map.ofEntries(
                    Map.entry("main", "patched@example.org")
            ));
        }
    };
    private static HsOfficeContactRealEntity PATCHED_CONTACT = HsOfficeContactRealEntity.builder()
            .uuid(PATCHED_CONTACT_UUID)
            .caption("Patched-Contact-Caption")
            .emailAddresses(Map.ofEntries(
                    Map.entry("main", "patched@example.org")
            ))
            .build();

    @Mock
    private EntityManagerWrapper emw;

    private StrictMapper mapper;


    @BeforeEach
    void init() {
        mapper = new StrictMapper(emw); // emw is injected after the constructor got called
        mapper.addConverter(
                    new HsOfficeContactFromResourceConverter<>(),
                    HsOfficeContactInsertResource.class, HsOfficeContactRealEntity.class);

        lenient().when(emw.getReference(HsOfficePersonRealEntity.class, PATCHED_HOLDER_UUID)).thenAnswer(
                p -> PATCHED_HOLDER);
        lenient().when(emw.getReference(HsOfficeContactRealEntity.class, PATCHED_CONTACT_UUID)).thenAnswer(
                p -> PATCHED_CONTACT);
    }

    @Override
    protected HsOfficeRelation newInitialEntity() {
        final var entity = new HsOfficeRelationRealEntity();
        entity.setUuid(INITIAL_RELATION_UUID);
        entity.setType(PARTNER);
        entity.setAnchor(HsOfficePersonRealEntity.builder()
                .uuid(INITIAL_ANCHOR_UUID)
                .personType(LEGAL_PERSON)
                .tradeName("Initial-Anchor-Tradename")
                .build());
        entity.setHolder(HsOfficePersonRealEntity.builder()
                .uuid(INITIAL_HOLDER_UUID)
                .personType(NATURAL_PERSON)
                .familyName("Initial-Holder-Family-Name")
                .givenName("Initial-Holder-Given-Name")
                .build());
        entity.setContact(HsOfficeContactRealEntity.builder()
                .uuid(INITIAL_CONTACT_UUID)
                .caption("Initial-Contact-Caption")
                .build());
        return entity;
    }

    @Override
    protected HsOfficeRelationPatchResource newPatchResource() {
        return new HsOfficeRelationPatchResource();
    }

    @Override
    protected HsOfficeRelationPatcher createPatcher(final HsOfficeRelation relation) {
        return new HsOfficeRelationPatcher(mapper, emw, relation);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "holderUuid",
                        HsOfficeRelationPatchResource::setHolderUuid,
                        PATCHED_HOLDER_UUID,
                        HsOfficeRelation::setHolder,
                        PATCHED_HOLDER),
                new SimpleProperty<>(
                        "holder",
                        HsOfficeRelationPatchResource::setHolder,
                        HOLDER_PATCH_RESOURCE,
                        HsOfficeRelation::setHolder,
                        withoutUuid(PATCHED_HOLDER))
                        .notNullable(),

                new JsonNullableProperty<>(
                        "contactUuid",
                        HsOfficeRelationPatchResource::setContactUuid,
                        PATCHED_CONTACT_UUID,
                        HsOfficeRelation::setContact,
                        PATCHED_CONTACT),
                new SimpleProperty<>(
                        "contact",
                        HsOfficeRelationPatchResource::setContact,
                        CONTACT_PATCH_RESOURCE,
                        HsOfficeRelation::setContact,
                        withoutUuid(PATCHED_CONTACT))
                        .notNullable()
        );
    }

    @Override
    protected void willPatchAllProperties() {
        // this generic test does not work because either holder or holder.uuid can be set
        assumeThat(true).isFalse();
    }

    @Test
    void willThrowExceptionIfHolderAndHolderUuidAreGiven() {
        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        patchResource.setHolderUuid(JsonNullable.of(PATCHED_HOLDER_UUID));
        patchResource.setHolder(HOLDER_PATCH_RESOURCE);

        // when
        final var exception = catchThrowable(() -> createPatcher(givenEntity).apply(patchResource));

        // then
        assertThat(exception).isInstanceOf(ValidationException.class)
                .hasMessage("either \"holder\" or \"holder.uuid\" can be given, not both");
    }

    @Test
    void willThrowExceptionIfContactAndContactUuidAreGiven() {
        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        patchResource.setContactUuid(JsonNullable.of(PATCHED_CONTACT_UUID));
        patchResource.setContact(CONTACT_PATCH_RESOURCE);

        // when
        final var exception =  catchThrowable(() -> createPatcher(givenEntity).apply(patchResource));

        // then
        assertThat(exception).isInstanceOf(ValidationException.class)
                .hasMessage("either \"contact\" or \"contact.uuid\" can be given, not both");
    }

    @Test
    void willPersistNewHolder() {
        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        patchResource.setHolder(HOLDER_PATCH_RESOURCE);

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        verify(emw, times(1)).persist(givenEntity.getHolder());
    }

    @Test
    void willPersistNewContact() {
        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        patchResource.setContact(CONTACT_PATCH_RESOURCE);

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        verify(emw, times(1)).persist(givenEntity.getContact());
    }

    private HsOfficePersonRealEntity withoutUuid(final HsOfficePersonRealEntity givenWithUuid) {
        return givenWithUuid.toBuilder().uuid(null).build();
    }

    private HsOfficeContactRealEntity withoutUuid(final HsOfficeContactRealEntity givenWithUuid) {
        return givenWithUuid.toBuilder().uuid(null).build();
    }
}
