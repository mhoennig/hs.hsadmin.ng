package net.hostsharing.hsadminng.hs.office.bankaccount;

import org.springframework.scheduling.TaskScheduler;
import java.time.Duration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.val;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@RbacDataJpaTest
@Tag("officeIntegrationTest")
class HsOfficeBankDirectoryImporterIntegrationTest {

    private static final String HEADER = "Bankleitzahl;Merkmal;Bezeichnung;PLZ;Ort;Kurzbezeichnung;PAN;BIC;"
            + "Prüfzifferberechnungsmethode;Datensatznummer;Änderungskennzeichen;Bankleitzahllöschung;Nachfolge-Bankleitzahl";

    private static final String FIRST_VERSION = HEADER + "\r\n"
            + "\"98010000\";\"1\";\"Beispielbank Nord\";\"12345\";\"Nordstadt\";\"Beispielbank\";;\"BSPNDEB0XXX\";\"09\";\"000001\";\"U\";\"0\";\"00000000\"\r\n"
            + "\"98010000\";\"2\";\"Beispielbank Nord Filiale\";\"12346\";\"Nordstadt\";\"Beispielbank\";;;\"09\";\"000002\";\"U\";\"0\";\"00000000\"\r\n"
            + "\"98020000\";\"1\";\"Müllerbank; Süd\";\"54321\";\"Südstadt\";\"Müllerbank\";;;\"09\";\"000003\";\"A\";\"0\";\"00000000\"\r\n"
            + "\"98030000\";\"1\";\"Gelöschte Bank\";\"54321\";\"Südstadt\";\"Gelöscht\";;\"GLSCDEB0XXX\";\"09\";\"000004\";\"D\";\"1\";\"98010000\"\r\n"
            + "\"99010000\";\"1\";\"Konflikt mit Demo-Bank\";\"54321\";\"Südstadt\";\"Konflikt\";;\"KNFLDEB0XXX\";\"09\";\"000005\";\"U\";\"0\";\"00000000\"\r\n";

    private static final String SECOND_VERSION = HEADER + "\r\n"
            + "\"98040000\";\"1\";\"Neue Beispielbank\";\"12345\";\"Nordstadt\";\"Neue\";;\"NEUBDEB0XXX\";\"09\";\"000001\";\"A\";\"0\";\"00000000\"\r\n";

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    final List<Object> publishedEvents = new ArrayList<>();
    WireMockServer bundesbank;
    HsOfficeBankDirectoryImporter importer;

    @BeforeEach
    void startBundesbank() {
        bundesbank = new WireMockServer(options().dynamicPort());
        bundesbank.start();
        importer = new HsOfficeBankDirectoryImporter(
                jdbc, transactionManager, mock(TaskScheduler.class), publishedEvents::add,
                URI.create(bundesbank.baseUrl() + "/blob/0/blz.csv"), Duration.ofMinutes(5), 5);
    }

    @AfterEach
    void stopBundesbank() {
        bundesbank.stop();
    }

    @Test
    void importsTheCurrentVersionOnlyOnceSupersedesSeededAndManualAndKeepsDemoBanks() {
        // given
        jdbc.update("insert into hs_office.bank (countryCode, bankCode, bic, source) values ('DE', '98020000', 'SEEDDEB0', 'MANUAL')");
        jdbc.update("insert into hs_office.bank (countryCode, bankCode, bic, source) values ('DE', '98010000', 'TYPODEB0', 'EXISTING')");
        givenCurrentVersion("hash1", FIRST_VERSION);

        // when
        importer.importIfNewVersion();
        importer.importIfNewVersion();

        // then
        assertThat(bundesbankRows()).containsExactlyInAnyOrder(
                "98010000|BSPNDEB0XXX|Beispielbank Nord",
                "98020000|null|Müllerbank; Süd");
        assertThat(count("source = 'DEMO' and bankCode = '99010000' and name = 'Demostadt Sparkasse'")).isEqualTo(1);
        assertThat(count("source = 'DEMO'")).isEqualTo(8);
        assertThat(count("source = 'MANUAL' and countryCode = 'DE'")).isZero();
        assertThat(count("source = 'EXISTING' and bankCode = '98010000'")).isZero();
        assertThat(importedUrls()).containsExactly(bundesbank.baseUrl() + "/blob/hash1/blz.csv");
        bundesbank.verify(1, getRequestedFor(urlEqualTo("/blob/hash1/blz.csv")));
        assertThat(publishedEvents).containsExactly(
                new HsOfficeBankDirectoryImporter.Imported(URI.create(bundesbank.baseUrl() + "/blob/hash1/blz.csv")));
    }

    @Test
    void keepsTheImportWithoutRetryingIfACheckAgainstItFails() {
        // given
        val scheduler = mock(TaskScheduler.class);
        val importerWithFailingCheck = new HsOfficeBankDirectoryImporter(
                jdbc, transactionManager, scheduler, event -> {
                    throw new IllegalStateException("check failed");
                },
                URI.create(bundesbank.baseUrl() + "/blob/0/blz.csv"), Duration.ofMinutes(5), 5);
        givenCurrentVersion("hash1", FIRST_VERSION);

        // when
        importerWithFailingCheck.importIfNewVersion();

        // then
        assertThat(importedUrls()).containsExactly(bundesbank.baseUrl() + "/blob/hash1/blz.csv");
        assertThat(bundesbankRows()).hasSize(2);
        verifyNoInteractions(scheduler);
    }

    @ParameterizedTest
    @ValueSource(strings = { "EXISTING", "MANUAL" })
    void removesABankCodeWhichTheBundesbankMarksAsDeleted(final String source) {
        // given
        jdbc.update("insert into hs_office.bank (countryCode, bankCode, bic, source) values ('DE', '98030000', 'GLSCDEB0XXX', ?::hs_office.BankSource)",
                source);
        givenCurrentVersion("hash1", FIRST_VERSION);

        // when
        importer.importIfNewVersion();

        // then
        assertThat(count("bankCode = '98030000'")).isZero();
    }

    @Test
    void replacesTheBundesbankRowsWithANewVersion() {
        // given
        givenCurrentVersion("hash1", FIRST_VERSION);
        importer.importIfNewVersion();
        givenCurrentVersion("hash2", SECOND_VERSION);

        // when
        importer.importIfNewVersion();

        // then
        assertThat(bundesbankRows()).containsExactly("98040000|NEUBDEB0XXX|Neue Beispielbank");
        assertThat(importedUrls()).containsExactlyInAnyOrder(
                bundesbank.baseUrl() + "/blob/hash1/blz.csv",
                bundesbank.baseUrl() + "/blob/hash2/blz.csv");
    }

    private void givenCurrentVersion(final String hash, final String csv) {
        bundesbank.stubFor(head(urlEqualTo("/blob/0/blz.csv")).willReturn(aResponse()
                .withStatus(302)
                .withHeader("Location", bundesbank.baseUrl() + "/blob/" + hash + "/blz.csv")));
        bundesbank.stubFor(get("/blob/" + hash + "/blz.csv").willReturn(aResponse()
                .withHeader("Content-Type", "text/csv")
                .withBody(csv.getBytes(StandardCharsets.ISO_8859_1))));
    }

    private List<String> bundesbankRows() {
        return jdbc.queryForList(
                "select bankCode || '|' || coalesce(bic, 'null') || '|' || name from hs_office.bank where source = 'BUNDESBANK'",
                String.class);
    }

    private List<String> importedUrls() {
        return jdbc.queryForList("select sourceUrl from hs_office.bank_directory_import where outcome = 'IMPORTED'", String.class);
    }

    private long count(final String condition) {
        val count = jdbc.queryForObject("select count(*) from hs_office.bank where " + condition, Long.class);
        return count == null ? 0 : count;
    }
}
