package net.hostsharing.hsadminng.hs.office.relationship;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationshipPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.test.PatchUnitTestBase;
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
class HsOfficeRelationshipEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeRelationshipPatchResource,
        HsOfficeRelationshipEntity
        > {

    static final UUID INITIAL_RELATIONSHIP_UUID = UUID.randomUUID();
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
    protected HsOfficeRelationshipEntity newInitialEntity() {
        final var entity = new HsOfficeRelationshipEntity();
        entity.setUuid(INITIAL_RELATIONSHIP_UUID);
        entity.setRelType(HsOfficeRelationshipType.REPRESENTATIVE);
        entity.setRelAnchor(givenInitialAnchorPerson);
        entity.setRelHolder(givenInitialHolderPerson);
        entity.setContact(givenInitialContact);
        return entity;
    }

    @Override
    protected HsOfficeRelationshipPatchResource newPatchResource() {
        return new HsOfficeRelationshipPatchResource();
    }

    @Override
    protected HsOfficeRelationshipEntityPatcher createPatcher(final HsOfficeRelationshipEntity relationship) {
        return new HsOfficeRelationshipEntityPatcher(em, relationship);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "contact",
                        HsOfficeRelationshipPatchResource::setContactUuid,
                        PATCHED_CONTACT_UUID,
                        HsOfficeRelationshipEntity::setContact,
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
