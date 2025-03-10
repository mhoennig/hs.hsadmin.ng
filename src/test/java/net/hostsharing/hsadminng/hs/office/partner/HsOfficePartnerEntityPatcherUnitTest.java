package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerDetailsPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
// This test class does not subclass PatchUnitTestBase because it has no directly patchable properties.
// But the factory-structure is kept, so PatchUnitTestBase could easily be plugged back in if needed.
class HsOfficePartnerEntityPatcherUnitTest {

    private static final UUID INITIAL_PARTNER_UUID = UUID.randomUUID();
    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();
    private static final UUID INITIAL_PARTNER_PERSON_UUID = UUID.randomUUID();
    private static final UUID INITIAL_DETAILS_UUID = UUID.randomUUID();

    private final HsOfficePersonRealEntity givenInitialPartnerPerson = HsOfficePersonRealEntity.builder()
            .uuid(INITIAL_PARTNER_PERSON_UUID)
            .build();
    private final HsOfficeContactRealEntity givenInitialContact = HsOfficeContactRealEntity.builder()
            .uuid(INITIAL_CONTACT_UUID)
            .build();

    private final HsOfficePartnerDetailsEntity givenInitialDetails = HsOfficePartnerDetailsEntity.builder()
            .uuid(INITIAL_DETAILS_UUID)
            .build();

    @Mock
    private EntityManagerWrapper emw;

    private StrictMapper mapper = new StrictMapper(emw);

    @BeforeEach
    void initMocks() {
        lenient().when(emw.getReference(eq(HsOfficePersonRealEntity.class), any())).thenAnswer(invocation ->
                HsOfficePersonRealEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(emw.getReference(eq(HsOfficeContactRealEntity.class), any())).thenAnswer(invocation ->
                HsOfficeContactRealEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Test
    void patchPartnerPerson() {
        // given
        final var patchResource = newPatchResource();
        final var newHolderUuid = UUID.randomUUID();
        patchResource.setPartnerRel(new HsOfficeRelationPatchResource());
        patchResource.getPartnerRel().setHolderUuid(JsonNullable.of(newHolderUuid));
        final var entity = newInitialEntity();

        // when
        createPatcher(entity).apply(patchResource);

        // then
        assertThat(entity.getPartnerRel().getHolder().getUuid()).isEqualTo(newHolderUuid);
    }

    @Test
    void patchPartnerContact() {
        // given
        final var patchResource = newPatchResource();
        final var newContactUuid = UUID.randomUUID();
        patchResource.setPartnerRel(new HsOfficeRelationPatchResource());
        patchResource.getPartnerRel().setContactUuid(JsonNullable.of(newContactUuid));
        final var entity = newInitialEntity();

        // when
        createPatcher(entity).apply(patchResource);

        // then
        assertThat(entity.getPartnerRel().getContact().getUuid()).isEqualTo(newContactUuid);
    }

    @Test
    void patchPartnerDetails() {
        // given
        final var patchResource = newPatchResource();
        final var newDateOfBirth = LocalDate.now();
        patchResource.setDetails(new HsOfficePartnerDetailsPatchResource());
        patchResource.getDetails().setDateOfDeath(JsonNullable.of(newDateOfBirth));
        final var entity = newInitialEntity();

        // when
        createPatcher(entity).apply(patchResource);

        // then
        assertThat(entity.getDetails().getDateOfDeath()).isEqualTo(newDateOfBirth);
    }

    protected HsOfficePartnerRbacEntity newInitialEntity() {
        final var entity = HsOfficePartnerRbacEntity.builder()
                .uuid(INITIAL_PARTNER_UUID)
                .partnerNumber(12345)
                .partnerRel(HsOfficeRelationRealEntity.builder()
                        .holder(givenInitialPartnerPerson)
                        .contact(givenInitialContact)
                        .build())
                .details(givenInitialDetails)
                .build();
        return entity;
    }

    protected HsOfficePartnerPatchResource newPatchResource() {
        return new HsOfficePartnerPatchResource();
    }

    protected HsOfficePartnerEntityPatcher createPatcher(final HsOfficePartnerRbacEntity partner) {
        return new HsOfficePartnerEntityPatcher(mapper, emw, partner);
    }
}
