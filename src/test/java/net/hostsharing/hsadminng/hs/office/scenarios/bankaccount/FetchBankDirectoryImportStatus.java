package net.hostsharing.hsadminng.hs.office.scenarios.bankaccount;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asAnonymous;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class FetchBankDirectoryImportStatus extends UseCase<FetchBankDirectoryImportStatus> {

    public FetchBankDirectoryImportStatus(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The bank-directory import runs unattended, thus a monitoring polls its status without any login
                and alerts if the last successful attempt gets older than the weekly interval;
                it just reports the number of valid SEPA mandates whose bank account does not match the bank directory.
                """);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch the Bank-Directory Import Status without any Login", () ->
                httpGet(asAnonymous(), "/api/hs/office/bank-directory/import-status?limit=3")
                        .expecting(OK).expecting(JSON));
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.<String>getFromBody("currentImport.sourceUrl"))
                .as("the current import names its version")
                .endsWith(ScenarioTest.resolve("/%{bundesbankVersion}/blz.csv", DROP_COMMENTS));
        assertThat(response.<String>getFromBody("lastSuccessfulRunAt"))
                .as("the last successful attempt is given").isNotBlank();
        val expectedSepaMandatesWithIssues = ScenarioTest.resolve("%{expectedSepaMandatesWithIssues???}", DROP_COMMENTS);
        if (expectedSepaMandatesWithIssues != null && !expectedSepaMandatesWithIssues.isEmpty()) {
            assertThat(response.<Integer>getFromBody("validSepaMandatesWithIssues"))
                    .as("the number of valid SEPA mandates with issues")
                    .isEqualTo(Integer.parseInt(expectedSepaMandatesWithIssues));
        }
        val expectedLatestOutcome = ScenarioTest.resolve("%{expectedLatestOutcome}", DROP_COMMENTS);
        assertThat(response.<String>getFromBody("recentRuns[0].outcome"))
                .as("the outcome of the latest attempt").isEqualTo(expectedLatestOutcome);
        if (expectedLatestOutcome.equals("FAILED")) {
            assertThat(response.<String>getFromBody("recentRuns[0].failure.request"))
                    .as("the failed request to the Bundesbank").matches("HEAD http.*/blz-aktuell.csv");
            assertThat(response.<Integer>getFromBody("recentRuns[0].failure.httpStatus"))
                    .as("the answer of the Bundesbank")
                    .isEqualTo(ScenarioTest.resolveTyped("%{expectedHttpStatus}", Integer.class));
        }
    }
}
