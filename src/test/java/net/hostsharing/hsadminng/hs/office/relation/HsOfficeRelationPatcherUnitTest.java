package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
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
class HsOfficeRelationPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeRelationPatchResource,
        HsOfficeRelation
        > {

    static final UUID INITIAL_RELATION_UUID = UUID.randomUUID();
    static final UUID PATCHED_CONTACT_UUID = UUID.randomUUID();

    @Mock
    EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeContactRealEntity.class), any())).thenAnswer(invocation ->
                HsOfficeContactRealEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    final HsOfficePersonEntity givenInitialAnchorPerson = HsOfficePersonEntity.builder()
            .uuid(UUID.randomUUID())
            .build();
    final HsOfficePersonEntity givenInitialHolderPerson = HsOfficePersonEntity.builder()
            .uuid(UUID.randomUUID())
            .build();
    final HsOfficeContactRealEntity givenInitialContact = HsOfficeContactRealEntity.builder()
            .uuid(UUID.randomUUID())
            .build();

    @Override
    protected HsOfficeRelation newInitialEntity() {
        final var entity = new HsOfficeRelationRbacEntity();
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
    protected HsOfficeRelationEntityPatcher createPatcher(final HsOfficeRelation relation) {
        return new HsOfficeRelationEntityPatcher(em, relation);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "contact",
                        HsOfficeRelationPatchResource::setContactUuid,
                        PATCHED_CONTACT_UUID,
                        HsOfficeRelation::setContact,
                        newContact(PATCHED_CONTACT_UUID))
                        .notNullable()
        );
    }

    static HsOfficeContactRealEntity newContact(final UUID uuid) {
        return HsOfficeContactRealEntity.builder().uuid(uuid).build();
    }
}
