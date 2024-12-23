package net.hostsharing.hsadminng.hs.office.membership;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.hypersistence.utils.hibernate.type.range.Range.localDateRange;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeMembershipController.class)
@Import({StandardMapper.class, DisableSecurityConfig.class})
@ActiveProfiles("test")
public class HsOfficeMembershipControllerRestTest {

    private static final HsOfficePartnerEntity PARTNER_12345 = HsOfficePartnerEntity.builder()
            .partnerNumber(12345)
            .build();
    public static final HsOfficeMembershipEntity MEMBERSHIP_1234501 = HsOfficeMembershipEntity.builder()
            .partner(PARTNER_12345)
            .memberNumberSuffix("01")
            .validity(localDateRange("[2013-10-01,]"))
            .status(HsOfficeMembershipStatus.ACTIVE)
            .build();
    public static final HsOfficeMembershipEntity MEMBERSHIP_1234500 = HsOfficeMembershipEntity.builder()
            .partner(PARTNER_12345)
            .memberNumberSuffix("00")
            .validity(localDateRange("[2011-04-01,2016-12-31]"))
            .status(HsOfficeMembershipStatus.CANCELLED)
            .build();
    public static final String MEMBERSHIP_1234501_JSON = """
            {
                "partner": {
                    "partnerNumber":"P-12345"
                },
                "memberNumber": "M-1234500",
                "memberNumberSuffix": "00",
                "validFrom": "2011-04-01",
                "validTo": "2016-12-30",
                "status":"CANCELLED"
            }
            """;

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
    class GetListOfMemberships {

        @Test
        void findMembershipByNonExistingPartnerNumberReturnsEmptyList() throws Exception {

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/memberships?partnerNumber=P-12345")
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

        @Test
        void findMembershipByExistingPartnerNumberReturnsAllRelatedMemberships() throws Exception {

            // given
            when(membershipRepo.findMembershipsByPartnerNumber(12345))
                    .thenReturn(List.of(
                            MEMBERSHIP_1234500,
                            MEMBERSHIP_1234501
                    ));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/memberships?partnerNumber=P-12345")
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
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    class GetSingleMembership {

        @Test
        void byUuid() throws Exception {

            // given
            final var givenUuid = UUID.randomUUID();
            when(membershipRepo.findByUuid(givenUuid)).thenReturn(
                    Optional.of(MEMBERSHIP_1234500)
            );

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/memberships/" + givenUuid)
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$", lenientlyEquals(MEMBERSHIP_1234501_JSON)));
        }

        @Test
        void byUnavailableUuid() throws Exception {

            // given
            when(membershipRepo.findByUuid(any(UUID.class))).thenReturn(
                    Optional.empty()
            );

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/memberships/" + UUID.randomUUID())
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }

        @Test
        void byMemberNumber() throws Exception {

            // given
            when(membershipRepo.findMembershipByMemberNumber(1234501)).thenReturn(
                    Optional.of(MEMBERSHIP_1234500)
            );

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/memberships/M-1234501")
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$", lenientlyEquals(MEMBERSHIP_1234501_JSON)));
        }

        @Test
        void byUnavailableMemberNumber() throws Exception {

            // given
            when(membershipRepo.findMembershipByMemberNumber(any(Integer.class))).thenReturn(
                    Optional.empty()
            );

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/memberships/M-0000000")
                            .header("current-subject", "superuser-alex@hostsharing.net")
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }

    }

    @Nested
    class PostNewMembership {

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
            TOO_SMALL("\"memberNumberSuffix\": \"9\",", "memberNumberSuffix must match \"[0-9]{2}\" but is \"9\""),
            TOO_LARGE("\"memberNumberSuffix\": \"100\",", "memberNumberSuffix must match \"[0-9]{2}\" but is \"100\""),
            NOT_NUMERIC("\"memberNumberSuffix\": \"AA\",", "memberNumberSuffix must match \"[0-9]{2}\" but is \"AA\""),
            EMPTY("\"memberNumberSuffix\": \"\",", "memberNumberSuffix must match \"[0-9]{2}\" but is \"\"");

            private final String memberNumberSuffixEntry;
            private final String expectedErrorMessage;

            InvalidMemberSuffixVariants(final String memberNumberSuffixEntry, final String expectedErrorMessage) {
                this.memberNumberSuffixEntry = memberNumberSuffixEntry;
                this.expectedErrorMessage = expectedErrorMessage;
            }
        }
    }

}
