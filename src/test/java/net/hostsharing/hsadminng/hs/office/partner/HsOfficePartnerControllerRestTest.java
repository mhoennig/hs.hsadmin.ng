package net.hostsharing.hsadminng.hs.office.partner;

import lombok.val;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactFromResourceConverter;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.SynchronizationType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficePartnerController.class)
@Import({ StrictMapper.class,
          JsonObjectMapperConfiguration.class,
          MessagesResourceConfig.class,
          MessageTranslator.class,
          HsOfficeContactFromResourceConverter.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficePartnerControllerRestTest {

    static final UUID GIVEN_MANDANTE_UUID = UUID.randomUUID();
    static final UUID GIVEN_PERSON_UUID = UUID.randomUUID();
    static final UUID GIVEN_CONTACT_UUID = UUID.randomUUID();
    static final UUID GIVEN_INVALID_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    HsOfficePartnerController controller;

    @MockitoBean
    Context contextMock;

    @Autowired
    MessageSource messageSource;

    @Autowired
    MessageTranslator translator;

    @MockitoBean
    HsOfficePartnerRbacRepository partnerRepo;

    @MockitoBean
    HsOfficeRelationRealRepository relationRepo;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManagerFactory emf;

    HsOfficePersonRealEntity mandate;

    HsOfficePersonRealEntity person;

    HsOfficeContactRealEntity contact;

    HsOfficePartnerRbacEntity partner;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(controller, "em", em);

        mandate = HsOfficePersonRealEntity.builder()
                .uuid(GIVEN_MANDANTE_UUID)
                .build();
        person = HsOfficePersonRealEntity.builder()
                .uuid(GIVEN_PERSON_UUID)
                .build();
        contact = HsOfficeContactRealEntity.builder()
                .uuid(GIVEN_CONTACT_UUID)
                .build();
        partner = givenPartner(UUID.randomUUID(), 12345);

        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);

        lenient().when(em.getReference(HsOfficePersonRealEntity.class, GIVEN_MANDANTE_UUID)).thenReturn(mandate);
        lenient().when(em.getReference(HsOfficePersonRealEntity.class, GIVEN_PERSON_UUID)).thenReturn(person);
        lenient().when(em.getReference(HsOfficeContactRealEntity.class, GIVEN_CONTACT_UUID)).thenReturn(contact);
        lenient().when(em.getReference(any(), eq(GIVEN_INVALID_UUID))).thenThrow(EntityNotFoundException.class);
    }

    @Nested
    class PostNewPartner {

        @Test
        void respondCreated_ifAllReferencesAreValid() throws Exception {
            // given
            val savedPartnerUuid = UUID.randomUUID();
            when(partnerRepo.save(any(HsOfficePartnerRbacEntity.class))).thenAnswer(invocation -> {
                final HsOfficePartnerRbacEntity saved = invocation.getArgument(0);
                saved.setUuid(savedPartnerUuid);
                return saved;
            });

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/office/partners")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {
                                        "partnerNumber": "P-20002",
                                        "partnerRel": {
                                             "anchor.uuid": "%s",
                                             "holder.uuid": "%s",
                                             "contact.uuid": "%s"
                                        },
                                        "details": {
                                            "registrationOffice": "Temp Registergericht Aurich",
                                            "registrationNumber": "111111"
                                        }
                                    }
                                    """.formatted(
                                    GIVEN_MANDANTE_UUID,
                                    GIVEN_PERSON_UUID,
                                    GIVEN_CONTACT_UUID))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("uuid", is(savedPartnerUuid.toString())))
                    .andExpect(jsonPath("partnerNumber", is("P-20002")));
        }

        @Test
        void respondBadRequest_ifPersonUuidIsInvalid() throws Exception {
            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/office/partners")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .header("Accept-Language", "de")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {
                                        "partnerNumber": "P-20002",
                                        "partnerRel": {
                                             "anchor.uuid": "%s",
                                             "holder.uuid": "%s",
                                             "contact.uuid": "%s"
                                        },
                                        "details": {
                                            "registrationOffice": "Temp Registergericht Aurich",
                                            "registrationNumber": "111111"
                                        }
                                    }
                                    """.formatted(
                                    GIVEN_MANDANTE_UUID,
                                    GIVEN_INVALID_UUID,
                                    GIVEN_CONTACT_UUID))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("statusCode", is(400)))
                    .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                    .andExpect(jsonPath("message", equalTo(
                            "ERROR: [400] RealPerson \"00000000-0000-0000-0000-000000000000\" nicht gefunden")));
        }

        @Test
        void respondBadRequest_ifContactUuidIsInvalid() throws Exception {
            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/office/partners")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {
                                        "partnerNumber": "P-20002",
                                        "partnerRel": {
                                             "anchor.uuid": "%s",
                                             "holder.uuid": "%s",
                                             "contact.uuid": "%s"
                                        },
                                        "details": {
                                            "registrationOffice": "Temp Registergericht Aurich",
                                            "registrationNumber": "111111"
                                        }
                                    }
                                    """.formatted(
                                    GIVEN_MANDANTE_UUID,
                                    GIVEN_PERSON_UUID,
                                    GIVEN_INVALID_UUID))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("statusCode", is(400)))
                    .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                    .andExpect(jsonPath("message", equalTo(
                            "ERROR: [400] RealContact \"00000000-0000-0000-0000-000000000000\" not found")));
        }
    }

    @Nested
    class GetSinglePartnerByPartnerNumber {

        @Test
        void respondWithPartnersByOptionalName() throws Exception {
            // given
            when(partnerRepo.findPartnerByOptionalNameLike("host")).thenReturn(List.of(givenPartner(UUID.randomUUID(), 12345)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/partners?name=host")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].partnerNumber", is("P-12345")));
        }

        @Test
        void respondWithPartner_ifUuidIsAvailable() throws Exception {
            // given
            val partnerUuid = UUID.randomUUID();
            when(partnerRepo.findByUuid(partnerUuid)).thenReturn(Optional.of(givenPartner(partnerUuid, 12345)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/partners/" + partnerUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(partnerUuid.toString())))
                    .andExpect(jsonPath("partnerNumber", is("P-12345")));
        }

        @Test
        void respondNotFound_ifUuidIsNotAvailable() throws Exception {
            // given
            val partnerUuid = UUID.randomUUID();
            when(partnerRepo.findByUuid(partnerUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/partners/" + partnerUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }

        @Test
        void respondWithPartner_ifPartnerNumberIsAvailable() throws Exception {
            // given
            when(partnerRepo.findPartnerByPartnerNumber(12345)).thenReturn(Optional.of(HsOfficePartnerRbacEntity.builder()
                    .partnerNumber(12345)
                    .build()));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/partners/P-12345")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("partnerNumber", is("P-12345")));
        }

        @Test
        void respondNotFound_ifPartnerNumberIsNotAvailable() throws Exception {
            // given
            when(partnerRepo.findPartnerByPartnerNumber(12345)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/partners/P-12345")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeletePartner {

        @Test
        void respondBadRequest_ifPartnerCannotBeDeleted() throws Exception {
            // given
            final UUID givenPartnerUuid = UUID.randomUUID();
            when(partnerRepo.findByUuid(givenPartnerUuid)).thenReturn(Optional.of(partner));
            when(partnerRepo.deleteByUuid(givenPartnerUuid)).thenReturn(0);

            final UUID givenRelationUuid = UUID.randomUUID();
            partner.setPartnerRel(HsOfficeRelationRealEntity.builder()
                    .uuid(givenRelationUuid)
                    .build());
            when(relationRepo.deleteByUuid(givenRelationUuid)).thenReturn(0);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/office/partners/" + givenPartnerUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isForbidden());
        }

        @Test
        void respondNotFound_ifPartnerDoesNotExist() throws Exception {
            // given
            final UUID givenPartnerUuid = UUID.randomUUID();
            when(partnerRepo.findByUuid(givenPartnerUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/office/partners/" + givenPartnerUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }

        @Test
        void respondNoContent_ifPartnerWasDeleted() throws Exception {
            // given
            final UUID givenPartnerUuid = UUID.randomUUID();
            when(partnerRepo.findByUuid(givenPartnerUuid)).thenReturn(Optional.of(partner));
            when(partnerRepo.deleteByUuid(givenPartnerUuid)).thenReturn(1);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/office/partners/" + givenPartnerUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void patchesPartnerDetails() throws Exception {
        // given
        val partnerUuid = UUID.randomUUID();
        val current = givenPartner(partnerUuid, 12345);
        when(partnerRepo.findByUuid(partnerUuid)).thenReturn(Optional.of(current));
        when(partnerRepo.save(any(HsOfficePartnerRbacEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/hs/office/partners/" + partnerUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                  {
                                      "details": {
                                          "registrationOffice": "Registergericht Hamburg",
                                          "registrationNumber": "222222"
                                      }
                                  }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("details.registrationOffice", is("Registergericht Hamburg")))
                .andExpect(jsonPath("details.registrationNumber", is("222222")));
    }

    private HsOfficePartnerRbacEntity givenPartner(final UUID uuid, final int partnerNumber) {
        return HsOfficePartnerRbacEntity.builder()
                .uuid(uuid)
                .partnerNumber(partnerNumber)
                .partnerRel(HsOfficeRelationRealEntity.builder()
                        .holder(HsOfficePersonRealEntity.builder()
                                .uuid(UUID.randomUUID())
                                .build())
                        .build())
                .details(HsOfficePartnerDetailsEntity.builder()
                        .registrationOffice("Registergericht Aurich")
                        .registrationNumber("111111")
                        .build())
                .build();
    }
}
