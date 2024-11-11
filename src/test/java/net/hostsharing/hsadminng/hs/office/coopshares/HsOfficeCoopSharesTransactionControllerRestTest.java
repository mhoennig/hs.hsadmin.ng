package net.hostsharing.hsadminng.hs.office.coopshares;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.rbac.test.JsonBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;
import java.util.function.Function;

import static net.hostsharing.hsadminng.rbac.test.JsonBuilder.jsonObject;
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
    StandardMapper mapper;

    @MockBean
    HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

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

    enum BadRequestTestCases {
        MEMBERSHIP_UUID_MISSING(
                requestBody -> requestBody.without("membership.uuid"),
                "[membershipUuid must not be null but is \"null\"]"),

        TRANSACTION_TYPE_MISSING(
                requestBody -> requestBody.without("transactionType"),
                "[transactionType must not be null but is \"null\"]"),

        VALUE_DATE_MISSING(
                requestBody -> requestBody.without("valueDate"),
                "[valueDate must not be null but is \"null\"]"),

        SHARES_COUNT_FOR_SUBSCRIPTION_MUST_BE_POSITIVE(
                requestBody -> requestBody
                        .with("transactionType", "SUBSCRIPTION")
                        .with("shareCount", -1),
                "[for SUBSCRIPTION, shareCount must be positive but is \"-1\"]"),

        SHARES_COUNT_FOR_CANCELLATION_MUST_BE_NEGATIVE(
                requestBody -> requestBody
                        .with("transactionType", "CANCELLATION")
                        .with("shareCount", 1),
                "[for CANCELLATION, shareCount must be negative but is \"1\"]"),

        SHARES_COUNT_MUST_NOT_BE_NULL(
                requestBody -> requestBody
                        .with("transactionType", "ADJUSTMENT")
                        .with("shareCount", 0),
                "[shareCount must not be 0 but is \"0\"]"),

        REFERENCE_MISSING(
                requestBody -> requestBody.without("reference"),
                "[reference must not be null but is \"null\"]"),

        REFERENCE_TOO_SHORT(
                requestBody -> requestBody.with("reference", "12345"),
                "[reference size must be between 6 and 48 but is \"12345\"]"),

        REFERENCE_TOO_LONG(
                requestBody -> requestBody.with("reference", "0123456789012345678901234567890123456789012345678"),
                "[reference size must be between 6 and 48 but is \"0123456789012345678901234567890123456789012345678\"]");

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
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCase.givenRequestBody())
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", is("ERROR: [400] " + testCase.expectedErrorMessage)));
    }

}
