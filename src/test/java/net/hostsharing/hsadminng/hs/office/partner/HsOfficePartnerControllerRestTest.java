package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactFromResourceConverter;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.SynchronizationType;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficePartnerController.class)
@Import({ StrictMapper.class, HsOfficeContactFromResourceConverter.class, DisableSecurityConfig.class})
@ActiveProfiles("test")
class HsOfficePartnerControllerRestTest {

    static final UUID GIVEN_MANDANTE_UUID = UUID.randomUUID();
    static final UUID GIVEN_PERSON_UUID = UUID.randomUUID();
    static final UUID GIVEN_CONTACT_UUID = UUID.randomUUID();
    static final UUID GIVEN_INVALID_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    HsOfficePartnerRbacRepository partnerRepo;

    @MockitoBean
    HsOfficeRelationRealRepository relationRepo;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManagerFactory emf;

    @Mock
    HsOfficePersonRealEntity mandateMock;

    @Mock
    HsOfficePersonRealEntity personMock;

    @Mock
    HsOfficeContactRbacEntity contactMock;

    @Mock
    HsOfficePartnerRbacEntity partnerMock;

    @BeforeEach
    void init() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);

        lenient().when(em.getReference(HsOfficePersonRealEntity.class, GIVEN_MANDANTE_UUID)).thenReturn(mandateMock);
        lenient().when(em.getReference(HsOfficePersonRealEntity.class, GIVEN_PERSON_UUID)).thenReturn(personMock);
        lenient().when(em.getReference(HsOfficeContactRbacEntity.class, GIVEN_CONTACT_UUID)).thenReturn(contactMock);
        lenient().when(em.getReference(any(), eq(GIVEN_INVALID_UUID))).thenThrow(EntityNotFoundException.class);
    }

    @Nested
    class PostNewPartner {

        @Test
        void respondBadRequest_ifPersonUuidIsInvalid() throws Exception {
            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/office/partners")
                            .header("current-subject", "superuser-alex@hostsharing.net")
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
                    .andExpect(jsonPath("message", startsWith("ERROR: [400] Cannot resolve HsOfficePersonRealEntity with uuid ")));
        }

        @Test
        void respondBadRequest_ifContactUuidIsInvalid() throws Exception {
            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/office/partners")
                            .header("current-subject", "superuser-alex@hostsharing.net")
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
                    .andExpect(jsonPath("message", startsWith("ERROR: [400] Cannot resolve HsOfficeContactRealEntity with uuid ")));
        }
    }

    @Nested
    class GetSinglePartnerByPartnerNumber {

        @Test
        void respondWithPartner_ifPartnerNumberIsAvailable() throws Exception {
            // given
            when(partnerRepo.findPartnerByPartnerNumber(12345)).thenReturn(Optional.of(HsOfficePartnerRbacEntity.builder()
                    .partnerNumber(12345)
                    .build()));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                    .get("/api/hs/office/partners/P-12345")
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
            when(partnerRepo.findByUuid(givenPartnerUuid)).thenReturn(Optional.of(partnerMock));
            when(partnerRepo.deleteByUuid(givenPartnerUuid)).thenReturn(0);

            final UUID givenRelationUuid = UUID.randomUUID();
            when(partnerMock.getPartnerRel()).thenReturn(HsOfficeRelationRealEntity.builder()
                    .uuid(givenRelationUuid)
                    .build());
            when(relationRepo.deleteByUuid(givenRelationUuid)).thenReturn(0);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/office/partners/" + givenPartnerUuid)
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isForbidden());
        }
    }
}
