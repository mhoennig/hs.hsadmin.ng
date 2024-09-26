package net.hostsharing.hsadminng.hs.office.bankaccount;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeBankAccountController.class)
class HsOfficeBankAccountControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Context contextMock;

    @MockBean
    StandardMapper mapper;

    @MockBean
    HsOfficeBankAccountRepository bankAccountRepo;

    enum InvalidIbanTestCase {
        TOO_SHORT("DE8810090000123456789", "[10090000123456789] length is 17, expected BBAN length is: 18"),
        TOO_LONG("DE8810090000123456789123445", "[10090000123456789123445] length is 23, expected BBAN length is: 18"),
        INVALID_CHARACTER("DE 8810090000123456789123445", "Iban's check digit should contain only digits."),
        INVALID_CHECKSUM(
                "DE88100900001234567893",
                "[DE88100900001234567893] has invalid check digit: 88, expected check digit is: 61");

        private final String givenIban;
        private final String expectedIbanMessage;

        InvalidIbanTestCase(final String givenIban, final String expectedErrorMessage) {
            this.givenIban = givenIban;
            this.expectedIbanMessage = expectedErrorMessage;
        }

        String givenIban() {
            return givenIban;
        }

        String expectedErrorMessage() {
            return expectedIbanMessage;
        }
    }

    @ParameterizedTest
    @EnumSource(InvalidIbanTestCase.class)
    void invalidIbanBeRejected(final InvalidIbanTestCase testCase) throws Exception {

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/bankaccounts")
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "holder": "new test holder",
                                    "iban": "%s",
                                    "bic": "BEVODEBB"
                                }
                                """.formatted(testCase.givenIban()))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", is("ERROR: [400] " + testCase.expectedErrorMessage())));
    }

    enum InvalidBicTestCase {
        TOO_SHORT("BEVODEB", "Bic length must be 8 or 11"),
        TOO_LONG("BEVODEBBX", "Bic length must be 8 or 11"),
        INVALID_CHARACTER("BEV-ODEB", "Bank code must contain only alphanumeric.");

        private final String givenBic;
        private final String expectedErrorMessage;

        InvalidBicTestCase(final String givenBic, final String expectedErrorMessage) {
            this.givenBic = givenBic;
            this.expectedErrorMessage = expectedErrorMessage;
        }

        String givenIban() {
            return givenBic;
        }

        String expectedErrorMessage() {
            return expectedErrorMessage;
        }
    }

    @ParameterizedTest
    @EnumSource(InvalidBicTestCase.class)
    void invalidBicBeRejected(final InvalidBicTestCase testCase) throws Exception {

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/bankaccounts")
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "holder": "new test holder",
                                    "iban": "DE88100900001234567892",
                                    "bic": "%s"
                                }
                                """.formatted(testCase.givenIban()))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", is("ERROR: [400] " + testCase.expectedErrorMessage())));
    }
}
