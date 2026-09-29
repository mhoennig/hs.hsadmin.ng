package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.val;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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

    @MockitoBean
    HsOfficeBankRepository bankRepo;

    @BeforeEach
    void initBankDirectory() {
        when(bankRepo.findByCountryCodeAndBankCode("DE", "99040000")).thenReturn(Optional.of(
                HsOfficeBankEntity.builder().countryCode("DE").bankCode("99040000").bic("DEMDDEB0")
                        .name("Direktbank Beispiel").source(HsOfficeBankEntity.Source.DEMO).build()));
        when(bankRepo.findByCountryCodeAndBankCode("DE", "99070000")).thenReturn(Optional.of(
                HsOfficeBankEntity.builder().countryCode("DE").bankCode("99070000")
                        .name("Zahlungsverkehrsstelle ohne BIC").source(HsOfficeBankEntity.Source.DEMO).build()));
        when(bankRepo.findByCountryCodeAndBankCode("DE", "99080000")).thenReturn(Optional.of(
                HsOfficeBankEntity.builder().countryCode("DE").bankCode("99080000").bic("DEMO-DE0")
                        .source(HsOfficeBankEntity.Source.MANUAL).build()));
        when(bankRepo.findByCountryCodeAndBankCode("AT", "99999")).thenReturn(Optional.of(
                HsOfficeBankEntity.builder().countryCode("AT").bankCode("99999").bic("DEMOATW0")
                        .name("Demobank Österreich").source(HsOfficeBankEntity.Source.DEMO).build()));
    }

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
                    .andExpect(jsonPath("$[0].iban", is("DE71990400001234567892")))
                    .andExpect(jsonPath("$[0].bic", is("DEMDDEB0")));
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
                    .andExpect(jsonPath("iban", is("DE71990400001234567892")))
                    .andExpect(jsonPath("bic", is("DEMDDEB0")));
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

    @Nested
    class PostNewBankAccount {

        @BeforeEach
        void saveReturnsEntityWithUuid() {
            when(bankAccountRepo.save(any(HsOfficeBankAccountEntity.class))).thenAnswer(invocation -> {
                final HsOfficeBankAccountEntity bankAccount = invocation.getArgument(0);
                bankAccount.setUuid(UUID.randomUUID());
                return bankAccount;
            });
        }

        @Test
        void createsGermanBankAccountWithMatchingBic() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE71990400001234567892",
                        "bic": "DEMDDEB0"
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("holder", is("Test Holder")))
                    .andExpect(jsonPath("iban", is("DE71990400001234567892")))
                    .andExpect(jsonPath("bic", is("DEMDDEB0")));
        }

        @Test
        void completesBicOfGermanBankAccountFromBankDirectory() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE71990400001234567892"
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("bic", is("DEMDDEB0")));
        }

        @Test
        void rejectsGermanBankAccountWithMismatchingBic() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE71990400001234567892",
                        "bic": "GENODEMIGLS"
                    }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message", is(
                            "ERROR: [400] BIC GENODEMIGLS does not match bank code 99040000 of IBAN DE71990400001234567892, expected DEMDDEB0")));
        }

        @Test
        void rejectsGermanIbanWithUnknownBankCode() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE34990200000000202051",
                        "bic": "DEMVDEH0"
                    }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message", is(
                            "ERROR: [400] IBAN DE34990200000000202051 has unknown bank code 99020000")));
        }

        @Test
        void acceptsUnknownGermanBankCodeForGlobalAdminWithOverrideBankDirectory() throws Exception {
            when(contextMock.hasGlobalAdminRole()).thenReturn(true);

            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE34990200000000202051",
                        "bic": "DEMVDEH0",
                        "overrideBankDirectory": true
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("bic", is("DEMVDEH0")));
        }

        @Test
        void rejectsOverrideBankDirectoryForUnknownGermanBankCodeForNonGlobalAdmin() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE34990200000000202051",
                        "bic": "DEMVDEH0",
                        "overrideBankDirectory": true
                    }
                    """)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("message", is("ERROR: [403] overrideBankDirectory requires global admin")));
        }

        @Test
        void requiresBicIfBankDirectoryHasNoBicForBankCode() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE57990700000000000000"
                    }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message", is("ERROR: [400] bic is required for IBAN DE57990700000000000000")));
        }

        @Test
        void rejectsCompletionWithInvalidBicFromBankDirectory() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE81990800000000000000"
                    }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message", is("ERROR: [400] bank directory has invalid BIC DEMO-DE0"
                            + " for bank code 99080000, bic is required for IBAN DE81990800000000000000")));
        }

        @Test
        void acceptsMismatchingBicForGlobalAdminWithOverrideBankDirectory() throws Exception {
            when(contextMock.hasGlobalAdminRole()).thenReturn(true);

            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE71990400001234567892",
                        "bic": "GENODEMIGLS",
                        "overrideBankDirectory": true
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("bic", is("GENODEMIGLS")));
        }

        @Test
        void rejectsOverrideBankDirectoryForNonGlobalAdmin() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE71990400001234567892",
                        "bic": "GENODEMIGLS",
                        "overrideBankDirectory": true
                    }
                    """)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("message", is("ERROR: [403] overrideBankDirectory requires global admin")));
        }

        @Test
        void acceptsOverrideBankDirectoryWithMatchingBicForNonGlobalAdmin() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "DE71990400001234567892",
                        "bic": "DEMDDEB0",
                        "overrideBankDirectory": true
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("bic", is("DEMDDEB0")));
        }

        @Test
        void createsForeignBankAccountWithStructurallyValidBicIfBankIsNotInDirectory() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "CH9300762011623852957",
                        "bic": "UBSWCHZH80A"
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("bic", is("UBSWCHZH80A")));
        }

        @Test
        void rejectsForeignBankAccountWithoutBicIfBankIsNotInDirectory() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "CH9300762011623852957"
                    }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message", is("ERROR: [400] bic is required for IBAN CH9300762011623852957")));
        }

        @Test
        void completesBicOfForeignBankAccountIfBankIsInDirectory() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "AT049999900000000001"
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("bic", is("DEMOATW0")));
        }

        @Test
        void rejectsForeignBankAccountWithMismatchingBicIfBankIsInDirectory() throws Exception {
            postBankAccount("""
                    {
                        "holder": "Test Holder",
                        "iban": "AT049999900000000001",
                        "bic": "GENODEMIGLS"
                    }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message", is(
                            "ERROR: [400] BIC GENODEMIGLS does not match bank code 99999 of IBAN AT049999900000000001, expected DEMOATW0")));
        }

        private ResultActions postBankAccount(final String json) throws Exception {
            return mockMvc.perform(MockMvcRequestBuilders
                    .post("/api/hs/office/bankaccounts")
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
                    .accept(MediaType.APPLICATION_JSON));
        }
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
        TOO_SHORT("DE8899040000123456789", "[99040000123456789] length is 17, expected BBAN length is: 18"),
        TOO_LONG("DE8899040000123456789123445", "[99040000123456789123445] length is 23, expected BBAN length is: 18"),
        INVALID_CHARACTER("DE 8899040000123456789123445", "Iban's check digit should contain only digits."),
        INVALID_CHECKSUM(
                "DE88990400001234567893",
                "[DE88990400001234567893] has invalid check digit: 88, expected check digit is: 44");

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
                                    "bic": "DEMDDEB0"
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
                                    "iban": "DE71990400001234567892",
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
                .iban("DE71990400001234567892")
                .bic("DEMDDEB0")
                .build();
    }
}
