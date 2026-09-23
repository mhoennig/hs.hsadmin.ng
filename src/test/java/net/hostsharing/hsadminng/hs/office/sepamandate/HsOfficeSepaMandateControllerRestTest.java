package net.hostsharing.hsadminng.hs.office.sepamandate;

import lombok.val;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRealEntity;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeSepaMandateController.class)
@Import({ StrictMapper.class,
          JsonObjectMapperConfiguration.class,
          MessagesResourceConfig.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficeSepaMandateControllerRestTest {

    @MockitoBean
    HsOfficeDebitorRepository debitorRepo;

    @MockitoBean
    HsOfficeBankAccountRepository bankAccountRepo;

    @MockitoBean
    HsOfficeSepaMandateRepository sepaMandateRepo;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @Nested
    class GetListOfSepaMandates {

        @Test
        void mapsSepaMandatesToResource() throws Exception {

            // given
            val givenPartner = HsOfficePartnerRealEntity.builder().partnerNumber(12345).build();
            val givenDebitor = HsOfficeDebitorEntity.builder()
                    .partner(givenPartner)
                    .debitorNumberSuffix("00")
                    .build();
            when(sepaMandateRepo.findSepaMandatesByOptionalIbanAndDebitorUuidAndValidAt("DE123", null, null)).thenReturn(
                    List.of(
                            HsOfficeSepaMandateEntity.builder()
                                    .agreement(LocalDate.of(2024, 10, 15))
                                    .debitor(givenDebitor)
                                    .reference("ref-xyz")
                                    .validity(Range.emptyRange(LocalDate.class))
                                    .build(),
                            HsOfficeSepaMandateEntity.builder()
                                    .agreement(LocalDate.of(2024, 11, 1))
                                    .debitor(givenDebitor)
                                    .reference("ref-abc")
                                    .validity(Range.localDateRange("[2024-11-01,)"))
                                    .build()
                    )
            );

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/sepamandates?iban=DE123")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andDo(print())
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$", lenientlyEquals("""
                            [
                              {
                                "debitor": { "debitorNumber": "D-1234500" },
                                "bankAccount": null,
                                "reference": "ref-xyz",
                                "agreement": "2024-10-15",
                                "validFrom": null,
                                "validTo": null
                              },
                              {
                                "debitor": { "debitorNumber": "D-1234500" },
                                "bankAccount": null,
                                "reference": "ref-abc",
                                "agreement": "2024-11-01",
                                "validFrom": "2024-11-01",
                                "validTo": null
                              }
                            ]
                            """)));
        }
    }

    @Nested
    class GetListOfSepaMandatesByDebitorAndValidAt {

        private static final UUID GIVEN_DEBITOR_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");
        private static final LocalDate GIVEN_KEY_DATE = LocalDate.of(2025, 1, 1);

        private final HsOfficeDebitorEntity givenDebitor = HsOfficeDebitorEntity.builder()
                .uuid(GIVEN_DEBITOR_UUID)
                .partner(HsOfficePartnerRealEntity.builder().partnerNumber(12345).build())
                .debitorNumberSuffix("00")
                .build();

        private void givenSepaMandateFor(final String iban, final UUID debitorUuid, final LocalDate validAt) {
            when(sepaMandateRepo.findSepaMandatesByOptionalIbanAndDebitorUuidAndValidAt(iban, debitorUuid, validAt))
                    .thenReturn(List.of(
                            HsOfficeSepaMandateEntity.builder()
                                    .agreement(LocalDate.of(2024, 10, 15))
                                    .debitor(givenDebitor)
                                    .reference("ref-valid")
                                    .validity(Range.localDateRange("[2024-11-01,)"))
                                    .build()));
        }

        @Test
        void passesDebitorUuidAndValidAtToTheRepository() throws Exception {
            // given
            givenSepaMandateFor(null, GIVEN_DEBITOR_UUID, GIVEN_KEY_DATE);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/sepamandates?debitorUuid=" + GIVEN_DEBITOR_UUID + "&validAt=2025-01-01")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", is(1)))
                    .andExpect(jsonPath("$[0].reference", is("ref-valid")));
        }

        @Test
        void resolvesDebitorNumberToTheDebitorUuid() throws Exception {
            // given
            when(debitorRepo.findDebitorByDebitorNumber(1234500)).thenReturn(Optional.of(givenDebitor));
            givenSepaMandateFor("DE123", GIVEN_DEBITOR_UUID, GIVEN_KEY_DATE);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/sepamandates?iban=DE123&debitorNumber=D-1234500&validAt=2025-01-01")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", is(1)))
                    .andExpect(jsonPath("$[0].debitor.debitorNumber", is("D-1234500")));
        }

        @Test
        void returnsEmptyListForUnknownDebitorNumber() throws Exception {
            // given
            when(debitorRepo.findDebitorByDebitorNumber(9999900)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/sepamandates?debitorNumber=D-9999900")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", is(0)));
        }

        @Test
        void rejectsDebitorUuidTogetherWithDebitorNumber() throws Exception {
            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/sepamandates?debitorUuid=" + GIVEN_DEBITOR_UUID + "&debitorNumber=D-1234500")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message", containsString("either debitorUuid or debitorNumber can be given, not both")));
        }
    }

    @Nested
    class GetSingleSepaMandate {

        @Test
        void returnsSepaMandateIfFound() throws Exception {
            // given
            val mandateUuid = UUID.randomUUID();
            when(sepaMandateRepo.findByUuid(mandateUuid)).thenReturn(Optional.of(givenSepaMandate(mandateUuid)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/sepamandates/" + mandateUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(mandateUuid.toString())))
                    .andExpect(jsonPath("debitor.debitorNumber", is("D-1234500")))
                    .andExpect(jsonPath("reference", is("ref-xyz")));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val mandateUuid = UUID.randomUUID();
            when(sepaMandateRepo.findByUuid(mandateUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/sepamandates/" + mandateUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void postsNewSepaMandate() throws Exception {
        // given
        val debitorUuid = UUID.randomUUID();
        val bankAccountUuid = UUID.randomUUID();
        val mandateUuid = UUID.randomUUID();
        when(debitorRepo.findByUuid(debitorUuid)).thenReturn(Optional.of(givenDebitor()));
        when(bankAccountRepo.findByUuid(bankAccountUuid)).thenReturn(Optional.of(givenBankAccount(bankAccountUuid)));
        when(sepaMandateRepo.save(any(HsOfficeSepaMandateEntity.class))).thenAnswer(invocation -> {
            final HsOfficeSepaMandateEntity mandate = invocation.getArgument(0);
            mandate.setUuid(mandateUuid);
            return mandate;
        });

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/sepamandates")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                  {
                                      "debitor.uuid": "%s",
                                      "bankAccount.uuid": "%s",
                                      "reference": "ref-created",
                                      "agreement": "2024-10-15",
                                      "validFrom": "2024-11-01",
                                      "validTo": "2025-10-31"
                                  }
                                """.formatted(debitorUuid, bankAccountUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(mandateUuid.toString())))
                .andExpect(jsonPath("reference", is("ref-created")))
                .andExpect(jsonPath("validTo", is("2025-10-31")));
    }

    @Nested
    class DeleteSepaMandate {

        @Test
        void respondsNoContentIfDeleted() throws Exception {
            // given
            val mandateUuid = UUID.randomUUID();
            when(sepaMandateRepo.deleteByUuid(mandateUuid)).thenReturn(1);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/office/sepamandates/" + mandateUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNoContent());
        }

        @Test
        void respondsNotFoundIfMissing() throws Exception {
            // given
            val mandateUuid = UUID.randomUUID();
            when(sepaMandateRepo.deleteByUuid(mandateUuid)).thenReturn(0);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/office/sepamandates/" + mandateUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void patchesSepaMandate() throws Exception {
        // given
        val mandateUuid = UUID.randomUUID();
        when(sepaMandateRepo.findByUuid(mandateUuid)).thenReturn(Optional.of(givenSepaMandate(mandateUuid)));
        when(sepaMandateRepo.save(any(HsOfficeSepaMandateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/hs/office/sepamandates/" + mandateUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                  {
                                      "reference": "ref-patched",
                                      "validTo": "2026-12-31"
                                  }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("reference", is("ref-patched")))
                .andExpect(jsonPath("validTo", is("2026-12-31")));
    }

    private HsOfficeSepaMandateEntity givenSepaMandate(final UUID mandateUuid) {
        return HsOfficeSepaMandateEntity.builder()
                .uuid(mandateUuid)
                .agreement(LocalDate.of(2024, 10, 15))
                .debitor(givenDebitor())
                .bankAccount(givenBankAccount(UUID.randomUUID()))
                .reference("ref-xyz")
                .validity(Range.localDateRange("[2024-11-01,)"))
                .build();
    }

    private HsOfficeDebitorEntity givenDebitor() {
        val givenPartner = HsOfficePartnerRealEntity.builder().partnerNumber(12345).build();
        return HsOfficeDebitorEntity.builder()
                .partner(givenPartner)
                .debitorNumberSuffix("00")
                .build();
    }

    private HsOfficeBankAccountEntity givenBankAccount(final UUID bankAccountUuid) {
        return HsOfficeBankAccountEntity.builder()
                .uuid(bankAccountUuid)
                .holder("Test Holder")
                .iban("DE123")
                .bic("GENODEF1HH2")
                .build();
    }
}
