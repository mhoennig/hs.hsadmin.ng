package net.hostsharing.hsadminng.hs.office.scenarios.bankaccount;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankDirectoryImporter;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static net.hostsharing.hsadminng.hs.scenarios.ExternalSystem.BUNDESBANK;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

public class AttemptBundesbankImportWhileUnavailable extends UseCase<AttemptBundesbankImportWhileUnavailable> {

    public AttemptBundesbankImportWhileUnavailable(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The weekly import fails if the Bundesbank does not answer as expected;
                it keeps the previous version and retries later.
                """);
    }

    @Override
    protected ScheduledJobRun runScheduledJob() {
        section("The Bundesbank Answers with an Error", () ->
                externalSystem(BUNDESBANK).fails("/blz-aktuell.csv",
                        ScenarioTest.resolveTyped("%{bundesbankHttpStatus}", Integer.class)));

        val jobRun = whenScheduledJobRuns("The Weekly Bank-Directory Import Runs", HsOfficeBankDirectoryImporter.class);
        keep("Bank Directory Import: Bundesbank answers %{bundesbankHttpStatus}",
                ScenarioTest.resolve("%{bundesbankHttpStatus}", DROP_COMMENTS));
        return jobRun;
    }

    @Override
    protected void verify(final ScheduledJobRun jobRun) {
        jobRun.expectingRequests(BUNDESBANK, "HEAD /blz-aktuell.csv")
                .expectingLog("bank directory import from .*: attempt 1 of \\d+ failed, next attempt in .*");
    }
}
