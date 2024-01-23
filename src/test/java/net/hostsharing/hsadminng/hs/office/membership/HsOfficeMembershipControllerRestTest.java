package net.hostsharing.hsadminng.hs.office.membership;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeMembershipController.class)
@Import(Mapper.class)
public class HsOfficeMembershipControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Context contextMock;

    @MockBean
    HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @MockBean
    HsOfficeMembershipRepository membershipRepo;

    @Mock
    EntityManager em;

    @MockBean
    EntityManagerFactory emf;

    @BeforeEach
    void init() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
    }

    @Test
    void respondBadRequest_ifPartnerUuidIsMissing() throws Exception {

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/memberships")
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                   {
                                       "partnerUuid": null,
                                       "mainDebitorUuid": "%s",
                                       "memberNumber": 20001,
                                       "validFrom": "2022-10-13",
                                       "membershipFeeBillable": "true"
                                     }
                                """.formatted(UUID.randomUUID()))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", is("[partnerUuid must not be null but is \"null\"]")));
    }

    @Test
    void respondBadRequest_ifDebitorUuidIsMissing() throws Exception {

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/memberships")
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                   {
                                       "partnerUuid": "%s",
                                       "mainDebitorUuid": null,
                                       "memberNumber": 20001,
                                       "validFrom": "2022-10-13",
                                       "membershipFeeBillable": "true"
                                     }
                                """.formatted(UUID.randomUUID()))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", is("[mainDebitorUuid must not be null but is \"null\"]")));
    }

    @Test
    void respondBadRequest_ifAnyGivenPartnerUuidCannotBeFound() throws Exception {

        // given
        final var givenPartnerUuid = UUID.randomUUID();
        final var givenMainDebitorUuid = UUID.randomUUID();
        when(em.find(HsOfficePartnerEntity.class, givenPartnerUuid)).thenReturn(null);
        when(em.find(HsOfficeDebitorEntity.class, givenMainDebitorUuid)).thenReturn(mock(HsOfficeDebitorEntity.class));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/memberships")
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                   {
                                       "partnerUuid": "%s",
                                       "mainDebitorUuid": "%s",
                                       "memberNumber": 20001,
                                       "validFrom": "2022-10-13",
                                       "membershipFeeBillable": "true"
                                     }
                                """.formatted(givenPartnerUuid, givenMainDebitorUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", is("Unable to find Partner with uuid " + givenPartnerUuid)));
    }

    @Test
    void respondBadRequest_ifAnyGivenDebitorUuidCannotBeFound() throws Exception {

        // given
        final var givenPartnerUuid = UUID.randomUUID();
        final var givenMainDebitorUuid = UUID.randomUUID();
        when(em.find(HsOfficePartnerEntity.class, givenPartnerUuid)).thenReturn(mock(HsOfficePartnerEntity.class));
        when(em.find(HsOfficeDebitorEntity.class, givenMainDebitorUuid)).thenReturn(null);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/memberships")
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                   {
                                       "partnerUuid": "%s",
                                       "mainDebitorUuid": "%s",
                                       "memberNumber": 20001,
                                       "validFrom": "2022-10-13",
                                       "membershipFeeBillable": "true"
                                     }
                                """.formatted(givenPartnerUuid, givenMainDebitorUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", is("Unable to find Debitor with uuid " + givenMainDebitorUuid)));
    }
}
