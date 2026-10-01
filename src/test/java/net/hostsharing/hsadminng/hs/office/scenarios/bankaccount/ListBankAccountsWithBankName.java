package net.hostsharing.hsadminng.hs.office.scenarios.bankaccount;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.PathAssertion;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

public class ListBankAccountsWithBankName extends UseCase<ListBankAccountsWithBankName> {

    public ListBankAccountsWithBankName(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle("Find the bank account of %{holder} with the name of its bank", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/bankaccounts?holder=" + uriEncoded("%{holder}"))
                        .expecting(expectedStatus).expecting(JSON));
    }

    @Override
    protected void verify(final HttpResponse response) {
        response.expectArrayElements(1);
        // an empty expectation means the bank is not in the directory, thus the response carries no bankName
        val expectedBankName = ScenarioTest.resolve("%{expectedBankName???}", DROP_COMMENTS);
        if (expectedBankName == null || expectedBankName.isEmpty()) {
            response.path("[0].bankName").isNull();
        } else {
            new PathAssertion("[0].bankName").contains("%{expectedBankName}").accept(response);
        }
    }
}
