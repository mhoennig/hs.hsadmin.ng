package net.hostsharing.hsadminng.hs.office.coopassets;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.Mapper;
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

@WebMvcTest(HsOfficeCoopAssetsTransactionController.class)
class HsOfficeCoopAssetsTransactionControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Context contextMock;

    @MockBean
    Mapper mapper;

    @MockBean
    HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    static final String VALID_INSERT_REQUEST_BODY = """
            {
               "membershipUuid": "%s",
               "transactionType": "DEPOSIT",
               "assetValue": 128.00,
               "valueDate": "2022-10-13",
               "reference": "valid reference",
               "comment": "valid comment"
            }
            """.formatted(UUID.randomUUID());

    enum BadRequestTestCases {
        MEMBERSHIP_UUID_MISSING(
                requestBody -> requestBody.without("membershipUuid"),
                "[membershipUuid must not be null but is \"null\"]"),

        TRANSACTION_TYPE_MISSING(
                requestBody -> requestBody.without("transactionType"),
                "[transactionType must not be null but is \"null\"]"),

        VALUE_DATE_MISSING(
                requestBody -> requestBody.without("valueDate"),
                "[valueDate must not be null but is \"null\"]"),

        ASSETS_VALUE_FOR_DEPOSIT_MUST_BE_POSITIVE(
                requestBody -> requestBody
                        .with("transactionType", "DEPOSIT")
                        .with("assetValue", -64.00),
                "[for DEPOSIT, assetValue must be positive but is \"-64.00\"]"),

        //TODO: other transaction types

        ASSETS_VALUE_FOR_DISBURSAL_MUST_BE_NEGATIVE(
                requestBody -> requestBody
                        .with("transactionType", "DISBURSAL")
                        .with("assetValue", 64.00),
                "[for DISBURSAL, assetValue must be negative but is \"64.00\"]"),

        //TODO: other transaction types

        ASSETS_VALUE_MUST_NOT_BE_NULL(
                requestBody -> requestBody
                        .with("transactionType", "ADJUSTMENT")
                        .with("assetValue", 0.00),
                "[assetValue must not be 0 but is \"0.00\"]"),

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
                        .post("/api/hs/office/coopassetstransactions")
                        .header("current-user", "superuser-alex@hostsharing.net")
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
