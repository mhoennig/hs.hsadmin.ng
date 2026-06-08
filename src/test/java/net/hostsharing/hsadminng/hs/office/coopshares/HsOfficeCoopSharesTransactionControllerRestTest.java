package net.hostsharing.hsadminng.hs.office.coopshares;

import lombok.val;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRealEntity;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;

import net.hostsharing.hsadminng.rbac.test.JsonBuilder;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.rbac.test.JsonBuilder.jsonObject;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeCoopSharesTransactionController.class)
@Import({ MessagesResourceConfig.class,
          StrictMapper.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficeCoopSharesTransactionControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

    @MockitoBean
    HsOfficeMembershipRepository membershipRepo;

    static final String VALID_INSERT_REQUEST_BODY = """
            {
               "membership.uuid": "%s",
               "transactionType": "SUBSCRIPTION",
               "shareCount": 8,
               "valueDate": "2022-10-13",
               "reference": "valid reference",
               "comment": "valid comment"
            }
            """.formatted(UUID.randomUUID());

    @Nested
    class GetListOfCoopShares {

        @Test
        void returnsTransactionsByMembershipAndDateRange() throws Exception {
            // given
            val membershipUuid = UUID.randomUUID();
            when(coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(
                    membershipUuid,
                    LocalDate.parse("2024-01-01"),
                    LocalDate.parse("2024-12-31")))
                    .thenReturn(List.of(givenShareTransaction(UUID.randomUUID(), membershipUuid)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/coopsharestransactions?membershipUuid=%s&fromValueDate=2024-01-01&toValueDate=2024-12-31"
                                    .formatted(membershipUuid))
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0]['membership.uuid']", is(membershipUuid.toString())))
                    .andExpect(jsonPath("$[0].transactionType", is("SUBSCRIPTION")))
                    .andExpect(jsonPath("$[0].shareCount", is(8)));
        }
    }

    @Nested
    class GetSingleCoopShare {

        @Test
        void returnsTransactionIfFound() throws Exception {
            // given
            val transactionUuid = UUID.randomUUID();
            val membershipUuid = UUID.randomUUID();
            when(coopSharesTransactionRepo.findByUuid(transactionUuid))
                    .thenReturn(Optional.of(givenShareTransaction(transactionUuid, membershipUuid)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/coopsharestransactions/" + transactionUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(transactionUuid.toString())))
                    .andExpect(jsonPath("$['membership.uuid']", is(membershipUuid.toString())));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val transactionUuid = UUID.randomUUID();
            when(coopSharesTransactionRepo.findByUuid(transactionUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/coopsharestransactions/" + transactionUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void postsNewCoopSharesTransaction() throws Exception {
        // given
        val membershipUuid = UUID.randomUUID();
        val transactionUuid = UUID.randomUUID();
        val membership = givenMembership(membershipUuid);
        when(membershipRepo.findByUuid(membershipUuid)).thenReturn(Optional.of(membership));
        when(coopSharesTransactionRepo.save(any(HsOfficeCoopSharesTransactionEntity.class))).thenAnswer(invocation -> {
            final HsOfficeCoopSharesTransactionEntity transaction = invocation.getArgument(0);
            transaction.setUuid(transactionUuid);
            return transaction;
        });

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/coopsharestransactions")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                   "membership.uuid": "%s",
                                   "transactionType": "SUBSCRIPTION",
                                   "shareCount": 8,
                                   "valueDate": "2022-10-13",
                                   "reference": "valid reference",
                                   "comment": "valid comment"
                                }
                                """.formatted(membershipUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(transactionUuid.toString())))
                .andExpect(jsonPath("$['membership.uuid']", is(membershipUuid.toString())));
    }

    enum BadRequestTestCases {
        MEMBERSHIP_UUID_MISSING(
                requestBody -> requestBody.without("membership.uuid"),
                "membershipUuid darf nicht null sein ist aber null"),

        TRANSACTION_TYPE_MISSING(
                requestBody -> requestBody.without("transactionType"),
                "transactionType darf nicht null sein ist aber null"),

        VALUE_DATE_MISSING(
                requestBody -> requestBody.without("valueDate"),
                "valueDate darf nicht null sein ist aber null"),

        SHARES_COUNT_FOR_SUBSCRIPTION_MUST_BE_POSITIVE(
                requestBody -> requestBody
                        .with("transactionType", "SUBSCRIPTION")
                        .with("shareCount", -1),
                "für transactionType=SUBSCRIPTION, muss shareCount positiv sein, ist aber -1"),

        SHARES_COUNT_FOR_CANCELLATION_MUST_BE_NEGATIVE(
                requestBody -> requestBody
                        .with("transactionType", "CANCELLATION")
                        .with("shareCount", 1),
                "für transactionType=CANCELLATION, muss shareCount negativ sein, ist aber 1"),

        SHARES_COUNT_MUST_NOT_BE_NULL(
                requestBody -> requestBody
                        .with("transactionType", "REVERSAL")
                        .with("shareCount", 0),
                "shareCount darf nicht 0 sein"),

        REFERENCE_MISSING(
                requestBody -> requestBody.without("reference"),
                "reference darf nicht null sein ist aber null"),

        REFERENCE_TOO_SHORT(
                requestBody -> requestBody.with("reference", "12345"),
                "reference Größe muss zwischen 6 und 48 sein ist aber \"12345\""),

        REFERENCE_TOO_LONG(
                requestBody -> requestBody.with("reference", "0123456789012345678901234567890123456789012345678"),
                "reference Größe muss zwischen 6 und 48 sein ist aber \"0123456789012345678901234567890123456789012345678\"");

        private final Function<JsonBuilder, JsonBuilder> givenBodyTransformation;
        private final String expectedErrorMessage;

        BadRequestTestCases(
                final Function<JsonBuilder, JsonBuilder> givenBodyTransformation,
                final String expectedErrorMessage) {
            this.givenBodyTransformation = givenBodyTransformation;
            this.expectedErrorMessage = expectedErrorMessage;
        }

        String givenRequestBody() {
            return givenBodyTransformation.apply(jsonObject(VALID_INSERT_REQUEST_BODY)).toString();
        }
    }

    @ParameterizedTest
    @EnumSource(BadRequestTestCases.class)
    void respondWithBadRequest(final BadRequestTestCases testCase) throws Exception {

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/coopsharestransactions")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCase.givenRequestBody())
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", containsString(testCase.expectedErrorMessage)));
    }

    private HsOfficeCoopSharesTransactionEntity givenShareTransaction(final UUID uuid, final UUID membershipUuid) {
        return HsOfficeCoopSharesTransactionEntity.builder()
                .uuid(uuid)
                .membership(givenMembership(membershipUuid))
                .transactionType(HsOfficeCoopSharesTransactionType.SUBSCRIPTION)
                .shareCount(8)
                .valueDate(LocalDate.parse("2022-10-13"))
                .reference("valid reference")
                .comment("valid comment")
                .build();
    }

    private HsOfficeMembershipEntity givenMembership(final UUID membershipUuid) {
        return HsOfficeMembershipEntity.builder()
                .uuid(membershipUuid)
                .partner(HsOfficePartnerRealEntity.builder()
                        .partnerNumber(12345)
                        .build())
                .memberNumberSuffix("00")
                .build();
    }
}
