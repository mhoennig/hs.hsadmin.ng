package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class ListSepaMandatesOfDebitorWithBankName extends UseCase<ListSepaMandatesOfDebitorWithBankName> {

    public ListSepaMandatesOfDebitorWithBankName(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                A member asks on the phone which of their bank accounts gets debited;
                the administration sees the name of the bank at each SEPA-mandate of the debitor.
                """);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch the SEPA-Mandates of the Debitor With the Names of Their Banks", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/sepamandates?debitorNumber=%{debitorNumber}")
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        final List<String> bankNames = response.getFromBody(
                "$[?(@.reference == '" + ScenarioTest.resolve("%{mandateReference}", DROP_COMMENTS) + "')].bankAccount.bankName");
        assertThat(bankNames).containsExactly(ScenarioTest.resolve("%{expectedBankName}", DROP_COMMENTS));
    }
}
