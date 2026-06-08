package net.hostsharing.hsadminng.hs.office.relation;

import lombok.val;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactFromResourceConverter;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeRelationController.class)
@Import({ StrictMapper.class,
          HsOfficeContactFromResourceConverter.class,
          JsonObjectMapperConfiguration.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficeRelationControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManagerFactory emf;

    @MockitoBean
    HsOfficeRelationRbacRepository relationRepo;

    @MockitoBean
    HsOfficePersonRealRepository personRepo;

    @MockitoBean
    HsOfficeContactRealRepository contactRepo;

    @BeforeEach
    void init() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
    }

    @Nested
    class GetListOfRelations {

        @Test
        void returnsRelations() throws Exception {
            // given
            val personUuid = UUID.randomUUID();
            when(relationRepo.findRelationRelatedToPersonUuidRelationTypeMarkPersonAndContactData(
                    personUuid, HsOfficeRelationType.REPRESENTATIVE, "billing", "miller", "office"))
                    .thenReturn(List.of(givenRelation(UUID.randomUUID())));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/relations")
                            .queryParam("personUuid", personUuid.toString())
                            .queryParam("relationType", "REPRESENTATIVE")
                            .queryParam("mark", "billing")
                            .queryParam("personData", "miller")
                            .queryParam("contactData", "office")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].type", is("REPRESENTATIVE")))
                    .andExpect(jsonPath("$[0].mark", is("billing")));
        }
    }

    @Nested
    class GetSingleRelation {

        @Test
        void returnsRelationIfFound() throws Exception {
            // given
            val relationUuid = UUID.randomUUID();
            when(relationRepo.findByUuid(relationUuid)).thenReturn(Optional.of(givenRelation(relationUuid)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/relations/" + relationUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(relationUuid.toString())))
                    .andExpect(jsonPath("type", is("REPRESENTATIVE")));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val relationUuid = UUID.randomUUID();
            when(relationRepo.findByUuid(relationUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/relations/" + relationUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void postsNewRelationWithExistingHolderAndContact() throws Exception {
        // given
        val anchor = givenPerson(UUID.randomUUID(), "Anchor GmbH");
        val holder = givenPerson(UUID.randomUUID(), "Holder GmbH");
        val contact = givenContact(UUID.randomUUID(), "holder contact");
        val savedRelationUuid = UUID.randomUUID();

        when(personRepo.findByUuid(anchor.getUuid())).thenReturn(Optional.of(anchor));
        when(personRepo.findByUuid(holder.getUuid())).thenReturn(Optional.of(holder));
        when(contactRepo.findByUuid(contact.getUuid())).thenReturn(Optional.of(contact));
        when(relationRepo.save(any(HsOfficeRelationRbacEntity.class))).thenAnswer(invocation -> {
            final HsOfficeRelationRbacEntity relation = invocation.getArgument(0);
            relation.setUuid(savedRelationUuid);
            return relation;
        });

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/relations")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "anchor.uuid": "%s",
                                    "holder.uuid": "%s",
                                    "type": "REPRESENTATIVE",
                                    "mark": "billing",
                                    "contact.uuid": "%s"
                                }
                                """.formatted(anchor.getUuid(), holder.getUuid(), contact.getUuid()))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(savedRelationUuid.toString())))
                .andExpect(jsonPath("type", is("REPRESENTATIVE")))
                .andExpect(jsonPath("mark", is("billing")));
    }

    @Test
    void deletesRelation() throws Exception {
        // given
        val relationUuid = UUID.randomUUID();
        when(relationRepo.deleteByUuid(relationUuid)).thenReturn(1);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/hs/office/relations/" + relationUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNoContent());
    }

    @Test
    void patchesRelationContact() throws Exception {
        // given
        val relationUuid = UUID.randomUUID();
        val newContactUuid = UUID.randomUUID();
        val current = givenRelation(relationUuid);
        val newContact = givenContact(newContactUuid, "new contact");

        when(relationRepo.findByUuid(relationUuid)).thenReturn(Optional.of(current));
        when(em.getReference(HsOfficeContactRealEntity.class, newContactUuid)).thenReturn(newContact);
        when(relationRepo.save(any(HsOfficeRelationRbacEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/hs/office/relations/" + relationUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "contact.uuid": "%s"
                                }
                                """.formatted(newContactUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid", is(relationUuid.toString())));
    }

    private HsOfficeRelationRbacEntity givenRelation(final UUID uuid) {
        return HsOfficeRelationRbacEntity.builder()
                .uuid(uuid)
                .type(HsOfficeRelationType.REPRESENTATIVE)
                .mark("billing")
                .anchor(givenPerson(UUID.randomUUID(), "Anchor GmbH"))
                .holder(givenPerson(UUID.randomUUID(), "Holder GmbH"))
                .contact(givenContact(UUID.randomUUID(), "holder contact"))
                .build();
    }

    private HsOfficePersonRealEntity givenPerson(final UUID uuid, final String tradeName) {
        return HsOfficePersonRealEntity.builder()
                .uuid(uuid)
                .personType(HsOfficePersonType.LEGAL_PERSON)
                .tradeName(tradeName)
                .build();
    }

    private HsOfficeContactRealEntity givenContact(final UUID uuid, final String caption) {
        return HsOfficeContactRealEntity.builder()
                .uuid(uuid)
                .caption(caption)
                .build();
    }
}
