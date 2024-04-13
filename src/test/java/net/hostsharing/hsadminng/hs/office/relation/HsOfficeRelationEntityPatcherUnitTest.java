package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.rbac.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsOfficeRelationEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeRelationPatchResource,
        HsOfficeRelationEntity
        > {

    static final UUID INITIAL_RELATION_UUID = UUID.randomUUID();
    static final UUID PATCHED_CONTACT_UUID = UUID.randomUUID();

    @Mock
    EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeContactEntity.class), any())).thenAnswer(invocation ->
                HsOfficeContactEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    final HsOfficePersonEntity givenInitialAnchorPerson = HsOfficePersonEntity.builder()
            .uuid(UUID.randomUUID())
            .build();
    final HsOfficePersonEntity givenInitialHolderPerson = HsOfficePersonEntity.builder()
            .uuid(UUID.randomUUID())
            .build();
    final HsOfficeContactEntity givenInitialContact = HsOfficeContactEntity.builder()
            .uuid(UUID.randomUUID())
            .build();

    @Override
    protected HsOfficeRelationEntity newInitialEntity() {
        final var entity = new HsOfficeRelationEntity();
        entity.setUuid(INITIAL_RELATION_UUID);
        entity.setType(HsOfficeRelationType.REPRESENTATIVE);
        entity.setAnchor(givenInitialAnchorPerson);
        entity.setHolder(givenInitialHolderPerson);
        entity.setContact(givenInitialContact);
        return entity;
    }

    @Override
    protected HsOfficeRelationPatchResource newPatchResource() {
        return new HsOfficeRelationPatchResource();
    }

    @Override
    protected HsOfficeRelationEntityPatcher createPatcher(final HsOfficeRelationEntity relation) {
        return new HsOfficeRelationEntityPatcher(em, relation);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "contact",
                        HsOfficeRelationPatchResource::setContactUuid,
                        PATCHED_CONTACT_UUID,
                        HsOfficeRelationEntity::setContact,
                        newContact(PATCHED_CONTACT_UUID))
                        .notNullable()
        );
    }

    static HsOfficeContactEntity newContact(final UUID uuid) {
        final var newContact = new HsOfficeContactEntity();
        newContact.setUuid(uuid);
        return newContact;
    }
}
