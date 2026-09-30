package net.hostsharing.hsadminng.hs.office.bankaccount;

import org.springframework.boot.convert.ApplicationConversionService;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.clearInvocations;
import java.util.ArrayList;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.time.Duration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.scheduling.TaskScheduler;
import org.mockito.ArgumentCaptor;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.val;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankDirectoryImporter.Bank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class HsOfficeBankDirectoryImporterUnitTest {

    @Nested
    class Activation {

        final ApplicationContextRunner context = new ApplicationContextRunner()
                // like in the application, e.g. to convert "PT5M" into a Duration
                .withInitializer(ctx -> ctx.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()))
                .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withBean(TaskScheduler.class, () -> mock(TaskScheduler.class))
                .withUserConfiguration(HsOfficeBankDirectoryImporter.class)
                .withPropertyValues(
                        "hsadminng.bank-directory.import.initial-delay=P1D",
                        "hsadminng.bank-directory.import.interval=P7D",
                        "hsadminng.bank-directory.import.retry-delay=PT5M",
                        "hsadminng.bank-directory.import.max-attempts=5");

        @Test
        void isActiveWithUrl() {
            context.withPropertyValues("hsadminng.bank-directory.import.url=https://bank-directory.example.org/blz.csv")
                    .run(ctx -> assertThat(ctx).hasSingleBean(HsOfficeBankDirectoryImporter.class));
        }

        @Test
        void isInactiveWithEmptyUrl() {
            context.withPropertyValues("hsadminng.bank-directory.import.url=")
                    .run(ctx -> assertThat(ctx).doesNotHaveBean(HsOfficeBankDirectoryImporter.class));
        }

        @Test
        void isInactiveWithoutUrl() {
            context.run(ctx -> assertThat(ctx).doesNotHaveBean(HsOfficeBankDirectoryImporter.class));
        }
    }

    private static final String HEADER = "Bankleitzahl;Merkmal;Bezeichnung;BIC;Änderungskennzeichen";

    @Nested
    class Parse {

        @Test
        void keepsTheRecordsOfBankCodesWhichAreNotDeletedAndNamesTheDeletedOnes() throws IOException {
            val banks = HsOfficeBankDirectoryImporter.parse(HEADER + "\r\n"
                    + "\"98010000\";\"1\";\"Beispielbank Nord \";\"BSPNDEB0XXX\";\"U\"\r\n"
                    + "\"98010000\";\"2\";\"Beispielbank Nord Filiale\";;\"U\"\r\n"
                    + "\"98020000\";\"1\";\"Müllerbank; \"\"Süd\"\"\";;\"A\"\r\n"
                    + "\"98030000\";\"1\";\"Gelöschte Bank\";\"GLSCDEB0XXX\";\"D\"\r\n"
                    + "\r\n");

            assertThat(banks.banks()).containsExactly(
                    new Bank("98010000", "BSPNDEB0XXX", "Beispielbank Nord"),
                    new Bank("98020000", null, "Müllerbank; \"Süd\""));
            assertThat(banks.deletedBankCodes()).containsExactly("98030000");
        }

        @Test
        void rejectsAnEmptyFile() {
            assertThatThrownBy(() -> HsOfficeBankDirectoryImporter.parse("\r\n"))
                    .isInstanceOf(IOException.class).hasMessage("empty bank directory");
        }

        @Test
        void rejectsAFileWithoutARequiredColumn() {
            assertThatThrownBy(() -> HsOfficeBankDirectoryImporter.parse("Bankleitzahl;Merkmal;Bezeichnung;SWIFT;Änderungskennzeichen"))
                    .isInstanceOf(IOException.class).hasMessageStartingWith("bank directory lacks some of the columns");
        }

        @Test
        void rejectsALineWithAnotherNumberOfFields() {
            assertThatThrownBy(() -> HsOfficeBankDirectoryImporter.parse(HEADER + "\n\"98010000\";\"1\";\"Beispielbank\""))
                    .isInstanceOf(IOException.class).hasMessageStartingWith("bank directory line with 3 instead of 5 fields");
        }

        @Test
        void rejectsAFileWithoutAnyBank() {
            assertThatThrownBy(() -> HsOfficeBankDirectoryImporter.parse(HEADER + "\n\"98010000\";\"2\";\"Filiale\";;\"U\""))
                    .isInstanceOf(IOException.class).hasMessage("bank directory without any bank");
        }
    }

    @Nested
    class ImportIfNewVersion {

        final JdbcTemplate jdbc = mock(JdbcTemplate.class);
        final TaskScheduler scheduler = mock(TaskScheduler.class);
        WireMockServer bundesbank;
        HsOfficeBankDirectoryImporter importer;

        @BeforeEach
        void startBundesbank() {
            bundesbank = new WireMockServer(options().dynamicPort());
            bundesbank.start();
            importer = new HsOfficeBankDirectoryImporter(
                    jdbc, mock(PlatformTransactionManager.class), scheduler,
                    URI.create(bundesbank.baseUrl() + "/blob/0/blz.csv"), Duration.ofMinutes(5), 5);
        }

        @AfterEach
        void stopBundesbank() {
            bundesbank.stop();
        }

        @Test
        void importsANewVersionFromTheRedirectTarget() {
            givenImported(false);
            givenRedirectTo("hash1");
            givenFile("/blob/hash1/blz.csv", 200);

            importer.importIfNewVersion();

            verify(jdbc).update("delete from hs_office.bank where source = 'BUNDESBANK'");
            verify(jdbc).batchUpdate(anyString(), eq(List.of(new Bank("98010000", "BSPNDEB0XXX", "Müllerbank"))), anyInt(),
                    any(ParameterizedPreparedStatementSetter.class));
            verify(jdbc).update(anyString(), eq(bundesbank.baseUrl() + "/blob/hash1/blz.csv"), eq(1));
        }

        @Test
        void importsTheConfiguredUrlItselfIfItDoesNotRedirect() {
            givenImported(false);
            bundesbank.stubFor(head(urlEqualTo("/blob/0/blz.csv")).willReturn(aResponse().withStatus(200)));
            givenFile("/blob/0/blz.csv", 200);

            importer.importIfNewVersion();

            verify(jdbc).update(anyString(), eq(bundesbank.baseUrl() + "/blob/0/blz.csv"), eq(1));
        }

        @Test
        void skipsAnAlreadyImportedVersion() {
            givenImported(true);
            givenRedirectTo("hash1");

            importer.importIfNewVersion();

            bundesbank.verify(0, getRequestedFor(urlEqualTo("/blob/hash1/blz.csv")));
            verify(jdbc, never()).batchUpdate(anyString(), anyList(), anyInt(), any(ParameterizedPreparedStatementSetter.class));
        }

        @Test
        void keepsThePreviousVersionIfTheUrlNeitherRedirectsNorExists() {
            bundesbank.stubFor(head(urlEqualTo("/blob/0/blz.csv")).willReturn(aResponse().withStatus(404)));

            importer.importIfNewVersion();

            verify(jdbc, never()).update(anyString());
        }

        @Test
        void keepsThePreviousVersionIfTheDownloadFails() {
            givenImported(false);
            givenRedirectTo("hash1");
            givenFile("/blob/hash1/blz.csv", 500);

            importer.importIfNewVersion();

            verify(jdbc, never()).update(anyString());
        }

        @Test
        void retriesWithExponentialBackoffAndLogsAnErrorAfterTheLastFailedAttempt(final CapturedOutput output) {
            bundesbank.stubFor(head(urlEqualTo("/blob/0/blz.csv")).willReturn(aResponse().withStatus(503)));

            importer.importIfNewVersion();
            val delays = new ArrayList<Duration>();
            for (int retry = 1; retry <= 4; ++retry) {
                runScheduledRetry(delays);
            }

            assertThat(delays).containsExactly(
                    Duration.ofMinutes(5), Duration.ofMinutes(10), Duration.ofMinutes(20), Duration.ofMinutes(40));
            verifyNoMoreInteractions(scheduler);
            assertThat(output).contains("attempt 4 of 5 failed, next attempt in PT40M")
                    .containsPattern("ERROR .*bank directory import from .* failed 5 times, the possibly outdated previous version stays");
        }

        @Test
        void stopsRetryingOnceAnAttemptSucceeds(final CapturedOutput output) {
            bundesbank.stubFor(head(urlEqualTo("/blob/0/blz.csv")).willReturn(aResponse().withStatus(503)));
            importer.importIfNewVersion();
            givenImported(false);
            givenRedirectTo("hash1");
            givenFile("/blob/hash1/blz.csv", 200);

            runScheduledRetry(new ArrayList<>());

            verify(jdbc).update(anyString(), eq(bundesbank.baseUrl() + "/blob/hash1/blz.csv"), eq(1));
            verifyNoMoreInteractions(scheduler);
            assertThat(output).doesNotContain("ERROR");
        }

        private void runScheduledRetry(final List<Duration> delays) {
            val retry = ArgumentCaptor.forClass(Runnable.class);
            val startTime = ArgumentCaptor.forClass(Instant.class);
            val before = Instant.now();
            verify(scheduler).schedule(retry.capture(), startTime.capture());
            clearInvocations(scheduler);
            delays.add(Duration.between(before, startTime.getValue()).plusSeconds(30).truncatedTo(ChronoUnit.MINUTES));
            retry.getValue().run();
        }

        private void givenImported(final boolean imported) {
            when(jdbc.queryForObject(anyString(), eq(Boolean.class), anyString())).thenReturn(imported);
        }

        private void givenRedirectTo(final String hash) {
            bundesbank.stubFor(head(urlEqualTo("/blob/0/blz.csv")).willReturn(aResponse()
                    .withStatus(302)
                    .withHeader("Location", "/blob/" + hash + "/blz.csv")));
        }

        private void givenFile(final String path, final int status) {
            bundesbank.stubFor(get(path).willReturn(aResponse()
                    .withStatus(status)
                    .withBody((HEADER + "\r\n\"98010000\";\"1\";\"Müllerbank\";\"BSPNDEB0XXX\";\"U\"\r\n")
                            .getBytes(StandardCharsets.ISO_8859_1))));
        }
    }
}
