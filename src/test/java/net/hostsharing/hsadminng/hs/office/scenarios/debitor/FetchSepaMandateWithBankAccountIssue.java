package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.office.scenarios.bankaccount.CreateBankAccount;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class FetchSepaMandateWithBankAccountIssue extends UseCase<FetchSepaMandateWithBankAccountIssue> {

    public FetchSepaMandateWithBankAccountIssue(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                A global admin can accept a BIC which does not match the bank directory;
                SEPA mandates with such a bank account show it as an issue.
                """);
    }

    @Override
    protected HttpResponse run() {
        new CreateBankAccount(testSuite, asSubject("hsh-alex_superuser").whichIs("a global admin"))
                .given("holder", ScenarioTest.resolve("%{bankAccountHolder}", DROP_COMMENTS))
                .given("iban", ScenarioTest.resolve("%{bankAccountIBAN}", DROP_COMMENTS))
                .given("bic", ScenarioTest.resolve("%{bankAccountBIC}", DROP_COMMENTS))
                .given("overrideBankDirectory", true)
                .expected("expectedBic", ScenarioTest.resolve("%{bankAccountBIC}", DROP_COMMENTS))
                .expected("expectedBankName", ScenarioTest.resolve("%{expectedBankName}", DROP_COMMENTS))
                .thenExpect(HttpStatus.CREATED)
                .keepAs(ScenarioTest.resolve("BankAccount: %{bankAccountHolder}", DROP_COMMENTS));

        obtain("SEPA-Mandate: %{bankAccountHolder}", () ->
                httpPost(asGlobalAgent(), "/api/hs/office/sepamandates", usingJsonBody("""
                        {
                           "debitor.uuid": ${Debitor: D-3101100 - Michelle Matthieu},
                           "bankAccount.uuid": ${BankAccount: %{bankAccountHolder}},
                           "reference": ${bankAccountHolder},
                           "agreement": "2026-09-01",
                           "validFrom": "2026-10-01"
                        }
                        """))
                        .expecting(CREATED).expecting(JSON));

        return withTitle("Fetch the SEPA Mandate", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/sepamandates/&{SEPA-Mandate: %{bankAccountHolder}}")
                        .reportWithResponse().expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        path("bankAccount.bic").contains("%{bankAccountBIC}").accept(response);
        path("bankAccount.issues.bankDirectoryMismatch.reason").contains("BIC_MISMATCH").accept(response);
        path("bankAccount.issues.bankDirectoryMismatch.bankCode").contains("%{expectedBankCode}").accept(response);
        path("bankAccount.issues.bankDirectoryMismatch.expectedBic").contains("%{expectedDirectoryBic}").accept(response);
    }
}
