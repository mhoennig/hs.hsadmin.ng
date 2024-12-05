package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
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
class HsOfficePartnerEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficePartnerPatchResource,
        HsOfficePartnerEntity
        > {

    private static final UUID INITIAL_PARTNER_UUID = UUID.randomUUID();
    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();
    private static final UUID INITIAL_PERSON_UUID = UUID.randomUUID();
    private static final UUID INITIAL_DETAILS_UUID = UUID.randomUUID();
    private static final UUID PATCHED_PARTNER_ROLE_UUID = UUID.randomUUID();

    private final HsOfficePersonRealEntity givenInitialPerson = HsOfficePersonRealEntity.builder()
            .uuid(INITIAL_PERSON_UUID)
            .build();
    private final HsOfficeContactRealEntity givenInitialContact = HsOfficeContactRealEntity.builder()
            .uuid(INITIAL_CONTACT_UUID)
            .build();

    private final HsOfficePartnerDetailsEntity givenInitialDetails = HsOfficePartnerDetailsEntity.builder()
            .uuid(INITIAL_DETAILS_UUID)
            .build();
    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeRelationRealEntity.class), any())).thenAnswer(invocation ->
                HsOfficeRelationRealEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsOfficePartnerEntity newInitialEntity() {
        final var entity = HsOfficePartnerEntity.builder()
                .uuid(INITIAL_PARTNER_UUID)
                .partnerNumber(12345)
                .partnerRel(HsOfficeRelationRealEntity.builder()
                        .holder(givenInitialPerson)
                        .contact(givenInitialContact)
                        .build())
                .details(givenInitialDetails)
                .build();
        return entity;
    }

    @Override
    protected HsOfficePartnerPatchResource newPatchResource() {
        return new HsOfficePartnerPatchResource();
    }

    @Override
    protected HsOfficePartnerEntityPatcher createPatcher(final HsOfficePartnerEntity partner) {
        return new HsOfficePartnerEntityPatcher(em, partner);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "partnerRel",
                        HsOfficePartnerPatchResource::setPartnerRelUuid,
                        PATCHED_PARTNER_ROLE_UUID,
                        HsOfficePartnerEntity::setPartnerRel,
                        newPartnerRel(PATCHED_PARTNER_ROLE_UUID))
                        .notNullable()
        );
    }

    private static HsOfficeRelationRealEntity newPartnerRel(final UUID uuid) {
        return HsOfficeRelationRealEntity.builder()
                    .uuid(uuid)
                    .build();
    }
}
