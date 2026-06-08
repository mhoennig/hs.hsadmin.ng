package net.hostsharing.hsadminng.hs.office.relation;

import lombok.val;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationContactPatchResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

import jakarta.persistence.EntityManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HsOfficeRelationEntityContactPatcherUnitTest {

    @Mock
    private EntityManager em;

    @Test
    void patchesContactByUuid() {
        // given
        val givenRelation = HsOfficeRelationRbacEntity.builder().build();
        val givenContactUuid = UUID.randomUUID();
        val givenContact = HsOfficeContactRealEntity.builder().uuid(givenContactUuid).build();
        val patchResource = new HsOfficeRelationContactPatchResource();
        patchResource.setContactUuid(JsonNullable.of(givenContactUuid));

        when(em.getReference(HsOfficeContactRealEntity.class, givenContactUuid)).thenReturn(givenContact);

        // when
        new HsOfficeRelationEntityContactPatcher(em, givenRelation).apply(patchResource);

        // then
        assertThat(givenRelation.getContact()).isSameAs(givenContact);
    }

    @Test
    void ignoresUndefinedContactUuid() {
        // given
        val givenContact = HsOfficeContactRealEntity.builder().uuid(UUID.randomUUID()).build();
        val givenRelation = HsOfficeRelationRbacEntity.builder()
                .contact(givenContact)
                .build();
        val patchResource = new HsOfficeRelationContactPatchResource();

        // when
        new HsOfficeRelationEntityContactPatcher(em, givenRelation).apply(patchResource);

        // then
        assertThat(givenRelation.getContact()).isSameAs(givenContact);
        verifyNoInteractions(em);
    }

    @Test
    void rejectsNullContactUuid() {
        // given
        val patchResource = new HsOfficeRelationContactPatchResource();
        patchResource.setContactUuid(JsonNullable.of(null));

        // when
        val exception = catchThrowable(() ->
                new HsOfficeRelationEntityContactPatcher(em, HsOfficeRelationRbacEntity.builder().build())
                        .apply(patchResource));

        // then
        assertThat(exception).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("property 'contact' must not be null");
    }
}
