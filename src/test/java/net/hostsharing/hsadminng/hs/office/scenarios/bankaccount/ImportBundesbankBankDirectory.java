package net.hostsharing.hsadminng.hs.office.scenarios.bankaccount;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankDirectoryImporter;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static net.hostsharing.hsadminng.hs.office.scenarios.bankaccount.BundesbankFile.bank;
import static net.hostsharing.hsadminng.hs.scenarios.ExternalSystem.BUNDESBANK;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

public class ImportBundesbankBankDirectory extends UseCase<ImportBundesbankBankDirectory> {

    public ImportBundesbankBankDirectory(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The Bundesbank publishes a new bank-code file every quarter;
                the backend imports it on its own, nobody triggers it,
                and checks the bank accounts of all SEPA mandates which are valid today or later.
                The required SEPA mandates and bank accounts were entered before the import, via overrideBankDirectory,
                because the bank directory did not know their banks.
                """);
    }

    @Override
    protected ScheduledJobRun runScheduledJob() {
        section("The Bundesbank Publishes Version %{bundesbankVersion}", () ->
                externalSystem(BUNDESBANK)
                        .redirects("/blz-aktuell.csv", "/%{bundesbankVersion}/blz.csv")
                        .serves("/%{bundesbankVersion}/blz.csv", BundesbankFile.CONTENT_TYPE, BundesbankFile.of(
                                bank("98050000", "Neue Beispielbank", "NEUBDEB0XXX"),
                                bank("98060000", "Fusionsbank", "FUSNDEFFXXX"))));

        val jobRun = whenScheduledJobRuns("The Weekly Bank-Directory Import Runs", HsOfficeBankDirectoryImporter.class);
        keep("Bank Directory: Bundesbank %{bundesbankVersion}", ScenarioTest.resolve("%{bundesbankVersion}", DROP_COMMENTS));
        return jobRun;
    }

    @Override
    protected void verify(final ScheduledJobRun jobRun) {
        jobRun.expectingRequests(BUNDESBANK, "HEAD /blz-aktuell.csv", "GET /%{bundesbankVersion}/blz.csv")
                .expectingLog("bank directory .*/%{bundesbankVersion}/blz.csv imported with %{expectedImportedBanks} banks")
                .expectingLog("%{expectedSepaMandatesWithIssues} valid SEPA mandates have a bank account "
                        + "which does not match the bank directory .*, GET /api/hs/office/sepamandates\\?issues=true lists them");
    }
}
