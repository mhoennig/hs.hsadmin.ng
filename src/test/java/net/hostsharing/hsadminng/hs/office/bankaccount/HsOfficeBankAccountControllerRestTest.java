package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.val;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeBankAccountController.class)
@Import({ StrictMapper.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficeBankAccountControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    HsOfficeBankAccountRepository bankAccountRepo;

    @Nested
    class GetListOfBankAccounts {

        @Test
        void returnsBankAccountsByOptionalHolder() throws Exception {
            // given
            when(bankAccountRepo.findByOptionalHolderLike("test holder"))
                    .thenReturn(List.of(givenBankAccount(UUID.randomUUID())));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/bankaccounts?holder=test holder")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].holder", is("Test Holder")))
                    .andExpect(jsonPath("$[0].iban", is("DE88100900001234567892")))
                    .andExpect(jsonPath("$[0].bic", is("BEVODEBB")));
        }
    }

    @Nested
    class GetSingleBankAccount {

        @Test
        void returnsBankAccountIfFound() throws Exception {
            // given
            val bankAccountUuid = UUID.randomUUID();
            when(bankAccountRepo.findByUuid(bankAccountUuid))
                    .thenReturn(Optional.of(givenBankAccount(bankAccountUuid)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/bankaccounts/" + bankAccountUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(bankAccountUuid.toString())))
                    .andExpect(jsonPath("holder", is("Test Holder")))
                    .andExpect(jsonPath("iban", is("DE88100900001234567892")))
                    .andExpect(jsonPath("bic", is("BEVODEBB")));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val bankAccountUuid = UUID.randomUUID();
            when(bankAccountRepo.findByUuid(bankAccountUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/bankaccounts/" + bankAccountUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void postsNewBankAccount() throws Exception {
        // given
        val bankAccountUuid = UUID.randomUUID();
        when(bankAccountRepo.save(any(HsOfficeBankAccountEntity.class))).thenAnswer(invocation -> {
            final HsOfficeBankAccountEntity bankAccount = invocation.getArgument(0);
            bankAccount.setUuid(bankAccountUuid);
            return bankAccount;
        });

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/bankaccounts")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "holder": "Test Holder",
                                    "iban": "DE88100900001234567892",
                                    "bic": "BEVODEBB"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(bankAccountUuid.toString())))
                .andExpect(jsonPath("holder", is("Test Holder")))
                .andExpect(jsonPath("iban", is("DE88100900001234567892")))
                .andExpect(jsonPath("bic", is("BEVODEBB")));
    }

    @Nested
    class DeleteBankAccount {

        @Test
        void respondsNoContentIfDeleted() throws Exception {
            // given
            val bankAccountUuid = UUID.randomUUID();
            when(bankAccountRepo.deleteByUuid(bankAccountUuid)).thenReturn(1);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/office/bankaccounts/" + bankAccountUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNoContent());
        }

        @Test
        void respondsNotFoundIfMissing() throws Exception {
            // given
            val bankAccountUuid = UUID.randomUUID();
            when(bankAccountRepo.deleteByUuid(bankAccountUuid)).thenReturn(0);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/office/bankaccounts/" + bankAccountUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

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
                        .header("Authorization", bearer("hsh-alex_superuser"))
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
                        .header("Authorization", bearer("hsh-alex_superuser"))
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

    private HsOfficeBankAccountEntity givenBankAccount(final UUID bankAccountUuid) {
        return HsOfficeBankAccountEntity.builder()
                .uuid(bankAccountUuid)
                .holder("Test Holder")
                .iban("DE88100900001234567892")
                .bic("BEVODEBB")
                .build();
    }
}
