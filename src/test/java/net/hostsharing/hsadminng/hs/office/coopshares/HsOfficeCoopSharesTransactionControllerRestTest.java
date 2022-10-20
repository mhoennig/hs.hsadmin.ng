package net.hostsharing.hsadminng.hs.office.coopshares;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeCoopSharesTransactionController.class)
class HsOfficeCoopSharesTransactionControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Context contextMock;

    @MockBean
    HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

    enum BadRequestTestCases {
        MEMBERSHIP_UUID_MISSING(
                """
                        {
                           "transactionType": "SUBSCRIPTION",
                           "sharesCount": 8,
                           "valueDate": "2022-10-13",
                           "reference": "temp ref A"
                         }
                        """,
                "[membershipUuid must not be null but is \"null\"]"),

        TRANSACTION_TYPE_MISSING(
                """
                        {
                           "membershipUuid": "%s",
                           "sharesCount": 8,
                           "valueDate": "2022-10-13",
                           "reference": "temp ref A"
                         }
                        """.formatted(UUID.randomUUID()),
                "[transactionType must not be null but is \"null\"]"),

        VALUE_DATE_MISSING(
                """
                        {
                           "membershipUuid": "%s",
                           "transactionType": "SUBSCRIPTION",
                           "sharesCount": 8,
                           "reference": "temp ref A"
                         }
                        """.formatted(UUID.randomUUID()),
                "[valueDate must not be null but is \"null\"]"),

        SHARES_COUNT_FOR_SUBSCRIPTION_MUST_BE_POSITIVE(
                """
                        {
                           "membershipUuid": "%s",
                           "transactionType": "SUBSCRIPTION",
                           "sharesCount": -1,
                           "valueDate": "2022-10-13",
                           "reference": "temp ref A"
                         }
                        """.formatted(UUID.randomUUID()),
                "[for SUBSCRIPTION, sharesCount must be positive but is \"-1\"]"),

        SHARES_COUNT_FOR_CANCELLATION_MUST_BE_NEGATIVE(
                """
                        {
                           "membershipUuid": "%s",
                           "transactionType": "CANCELLATION",
                           "sharesCount": 1,
                           "valueDate": "2022-10-13",
                           "reference": "temp ref A"
                         }
                        """.formatted(UUID.randomUUID()),
                "[for CANCELLATION, sharesCount must be negative but is \"1\"]"),

        SHARES_COUNT_MUST_NOT_BE_NULL(
                """
                        {
                           "membershipUuid": "%s",
                           "transactionType": "ADJUSTMENT",
                           "sharesCount": 0,
                           "valueDate": "2022-10-13",
                           "reference": "temp ref A"
                         }
                        """.formatted(UUID.randomUUID()),
                "[sharesCount must not be 0 but is \"0\"]"),

        REFERENCE_MISSING(
                """
                        {
                           "membershipUuid": "%s",
                           "transactionType": "SUBSCRIPTION",
                           "sharesCount": 8,
                           "valueDate": "2022-10-13"
                         }
                        """.formatted(UUID.randomUUID()),
                "[reference must not be null but is \"null\"]"),

        REFERENCE_TOO_SHORT(
                """
                        {
                           "membershipUuid": "%s",
                           "transactionType": "SUBSCRIPTION",
                           "sharesCount": 8,
                           "valueDate": "2022-10-13",
                           "reference": "12345"
                         }
                        """.formatted(UUID.randomUUID()),
                "[reference size must be between 6 and 48 but is \"12345\"]"),

        REFERENCE_TOO_LONG(
                """
                        {
                           "membershipUuid": "%s",
                           "transactionType": "SUBSCRIPTION",
                           "sharesCount": 8,
                           "valueDate": "2022-10-13",
                           "reference": "0123456789012345678901234567890123456789012345678"
                         }
                        """.formatted(UUID.randomUUID()),
                "[reference size must be between 6 and 48 but is \"0123456789012345678901234567890123456789012345678\"]");

        private final String givenBody;
        private final String expectedErrorMessage;

        BadRequestTestCases(final String givenBody, final String expectedErrorMessage) {
            this.givenBody = givenBody;
            this.expectedErrorMessage = expectedErrorMessage;
        }
    }

    @ParameterizedTest
    @EnumSource(BadRequestTestCases.class)
    void respondWithBadRequest(final BadRequestTestCases testCase) throws Exception {

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/coopsharestransactions")
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCase.givenBody)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("status", is(400)))
                .andExpect(jsonPath("error", is("Bad Request")))
                .andExpect(jsonPath("message", is(testCase.expectedErrorMessage)));
    }

}
