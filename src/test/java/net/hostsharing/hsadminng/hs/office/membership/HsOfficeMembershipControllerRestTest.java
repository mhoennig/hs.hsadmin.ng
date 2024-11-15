package net.hostsharing.hsadminng.hs.office.membership;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeMembershipController.class)
@Import(StandardMapper.class)
public class HsOfficeMembershipControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Context contextMock;

    @MockBean
    HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @MockBean
    HsOfficeMembershipRepository membershipRepo;

    @MockBean
    EntityManagerWrapper em;

    @Nested
    class GetMemberships {

        @Test
        void findMembershipByNonExistingMemberNumberReturnsEmptyList() throws Exception {

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/memberships?memberNumber=12345")
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                       {
                                           "partner.uuid": null,
                                           "memberNumberSuffix": "01",
                                           "validFrom": "2022-10-13",
                                           "membershipFeeBillable": "true"
                                         }
                                    """)
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class AddMembership {

        @Test
        void respondBadRequest_ifPartnerUuidIsMissing() throws Exception {

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/office/memberships")
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                       {
                                           "partner.uuid": null,
                                           "memberNumberSuffix": "01",
                                           "validFrom": "2022-10-13",
                                           "membershipFeeBillable": "true"
                                         }
                                    """)
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("statusCode", is(400)))
                    .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                    // FYI: the brackets around the message are here because it's actually an array, in this case of size 1
                    .andExpect(jsonPath("message", is("ERROR: [400] [partnerUuid must not be null but is \"null\"]")));
        }

        @Test
        void respondBadRequest_ifAnyGivenPartnerUuidCannotBeFound() throws Exception {

            // given
            final var givenPartnerUuid = UUID.randomUUID();
            when(em.find(HsOfficePartnerEntity.class, givenPartnerUuid)).thenReturn(null);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/office/memberships")
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                       {
                                           "partner.uuid": "%s",
                                           "memberNumberSuffix": "01",
                                           "validFrom": "2022-10-13",
                                           "membershipFeeBillable": "true"
                                         }
                                    """.formatted(givenPartnerUuid))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("statusCode", is(400)))
                    .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                    .andExpect(jsonPath(
                            "message",
                            is("ERROR: [400] Unable to find Partner by partner.uuid: " + givenPartnerUuid)));
        }

        @ParameterizedTest
        @EnumSource(InvalidMemberSuffixVariants.class)
        void respondBadRequest_ifMemberNumberSuffixIsInvalid(final InvalidMemberSuffixVariants testCase) throws Exception {

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/office/memberships")
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                       {
                                           "partner.uuid": "%s",
                                           %s
                                           "validFrom": "2022-10-13",
                                           "membershipFeeBillable": "true"
                                         }
                                    """.formatted(UUID.randomUUID(), testCase.memberNumberSuffixEntry))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("statusCode", is(400)))
                    .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                    .andExpect(jsonPath("message", containsString(testCase.expectedErrorMessage)));
        }

        public enum InvalidMemberSuffixVariants {
            MISSING("", "[memberNumberSuffix must not be null but is \"null\"]"),
            TOO_SMALL("\"memberNumberSuffix\": \"9\",", "memberNumberSuffix size must be between 2 and 2 but is \"9\""),
            TOO_LARGE("\"memberNumberSuffix\": \"100\",", "memberNumberSuffix size must be between 2 and 2 but is \"100\""),
            NOT_NUMERIC("\"memberNumberSuffix\": \"AA\",", "memberNumberSuffix must match \"[0-9]+\" but is \"AA\""),
            EMPTY("\"memberNumberSuffix\": \"\",", "memberNumberSuffix size must be between 2 and 2 but is \"\"");

            private final String memberNumberSuffixEntry;
            private final String expectedErrorMessage;

            InvalidMemberSuffixVariants(final String memberNumberSuffixEntry, final String expectedErrorMessage) {
                this.memberNumberSuffixEntry = memberNumberSuffixEntry;
                this.expectedErrorMessage = expectedErrorMessage;
            }
        }
    }

}
