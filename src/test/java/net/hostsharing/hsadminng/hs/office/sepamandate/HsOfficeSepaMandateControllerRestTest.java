package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRealEntity;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
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

import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeSepaMandateController.class)
@Import({ StrictMapper.class, DisableSecurityConfig.class, MessageTranslator.class})
@ActiveProfiles("test")
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
            final var givenPartner = HsOfficePartnerRealEntity.builder().partnerNumber(12345).build();
            final var givenDebitor = HsOfficeDebitorEntity.builder()
                    .partner(givenPartner)
                    .debitorNumberSuffix("00")
                    .build();
            when(sepaMandateRepo.findSepaMandateByOptionalIban("DE123")).thenReturn(
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
                            .header("Authorization", "Bearer superuser-alex@hostsharing.net")
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
}
