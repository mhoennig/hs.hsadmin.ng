package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.val;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeBankDirectoryController.class)
@Import({ MessageTranslator.class, WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficeBankDirectoryControllerRestTest {

    private static final String SOURCE_URL = "https://bundesbank.example/2026-09/blz.csv";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HsOfficeBankRepository bankRepo;

    @MockitoBean
    HsOfficeBankDirectoryController.SepaMandateIssues sepaMandateIssues;

    @Test
    void returnsTheCurrentImportAndTheRecentRunsWithoutAuthentication() throws Exception {
        // given
        when(bankRepo.findLastImport()).thenReturn(Optional.of(lastImport()));
        when(bankRepo.findLastSuccessfulImportRunAt()).thenReturn(Optional.of(Instant.parse("2026-09-28T01:00:09Z")));
        when(sepaMandateIssues.countValidSepaMandatesWithIssues()).thenReturn(2);
        when(bankRepo.findRecentImportRuns(2)).thenReturn(List.of(
                run("2026-09-29T01:00:00Z", "FAILED", null, null, "HTTP_ERROR", "HEAD https://bundesbank.example/blz.csv", 503, null),
                run("2026-09-28T01:00:09Z", "UNCHANGED", SOURCE_URL, null, null, null, null, null)));

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/hs/office/bank-directory/import-status")
                .param("limit", "2")
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("currentImport.sourceUrl", is(SOURCE_URL)))
                .andExpect(jsonPath("currentImport.importedAt", startsWith("2026-09-07T01:00:12")))
                .andExpect(jsonPath("currentImport.bankCount", is(3345)))
                .andExpect(jsonPath("lastSuccessfulRunAt", startsWith("2026-09-28T01:00:09")))
                .andExpect(jsonPath("recentRuns", hasSize(2)))
                .andExpect(jsonPath("recentRuns[0].outcome", is("FAILED")))
                .andExpect(jsonPath("recentRuns[0].failure.kind", is("HTTP_ERROR")))
                .andExpect(jsonPath("recentRuns[0].failure.request", is("HEAD https://bundesbank.example/blz.csv")))
                .andExpect(jsonPath("recentRuns[0].failure.httpStatus", is(503)))
                .andExpect(jsonPath("recentRuns[1].outcome", is("UNCHANGED")))
                .andExpect(jsonPath("recentRuns[1].sourceUrl", is(SOURCE_URL)))
                .andExpect(jsonPath("recentRuns[1].failure").value(nullValue()))
                .andExpect(jsonPath("validSepaMandatesWithIssues", is(2)));
    }

    @Test
    void returnsNoImportIfThereWasNone() throws Exception {
        // given
        when(bankRepo.findLastImport()).thenReturn(Optional.empty());
        when(bankRepo.findLastSuccessfulImportRunAt()).thenReturn(Optional.empty());
        when(bankRepo.findRecentImportRuns(10)).thenReturn(List.of());

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/hs/office/bank-directory/import-status")
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("currentImport").value(nullValue()))
                .andExpect(jsonPath("lastSuccessfulRunAt").value(nullValue()))
                .andExpect(jsonPath("recentRuns", hasSize(0)));
    }

    @Test
    void rejectsALimitAboveTheMaximum() throws Exception {
        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/hs/office/bank-directory/import-status")
                .param("limit", "101")
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());
    }

    private static HsOfficeBankRepository.LastImport lastImport() {
        return new HsOfficeBankRepository.LastImport() {

            @Override
            public String getSourceUrl() {
                return SOURCE_URL;
            }

            @Override
            public Instant getImportedAt() {
                return Instant.parse("2026-09-07T01:00:12Z");
            }

            @Override
            public Integer getBankCount() {
                return 3345;
            }
        };
    }

    private static HsOfficeBankRepository.ImportRun run(
            final String startedAt, final String outcome, final String sourceUrl, final Integer bankCount,
            final String failureKind, final String failureRequest, final Integer failureHttpStatus, final Integer failureLine) {
        return new HsOfficeBankRepository.ImportRun() {

            @Override
            public Instant getStartedAt() {
                return Instant.parse(startedAt);
            }

            @Override
            public String getOutcome() {
                return outcome;
            }

            @Override
            public String getSourceUrl() {
                return sourceUrl;
            }

            @Override
            public Integer getBankCount() {
                return bankCount;
            }

            @Override
            public String getFailureKind() {
                return failureKind;
            }

            @Override
            public String getFailureRequest() {
                return failureRequest;
            }

            @Override
            public Integer getFailureHttpStatus() {
                return failureHttpStatus;
            }

            @Override
            public Integer getFailureLine() {
                return failureLine;
            }
        };
    }
}
