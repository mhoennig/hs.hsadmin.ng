package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class ListValidSepaMandatesWithoutIssues extends BillingUseCase<ListValidSepaMandatesWithoutIssues> {

    public ListValidSepaMandatesWithoutIssues(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The billing system decides itself whether it skips SEPA-mandates whose bank account has `issues` or reacts to them.
                With `issues=false` it fetches only those SEPA-mandates without issues, without `issues` it fetches all.
                """ + API_KEY_HINT);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch the SEPA-Mandates Without Issues Which Are Valid at the Key Date", () ->
                httpGet(asBillingSystem(), "/api/hs/office/sepamandates?validAt=%{validAt}&issues=false")
                        .limitReportedResultTo(1)
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final UseCase<ListValidSepaMandatesWithoutIssues>.HttpResponse response) {
        final List<Object> issues = response.getFromBody("$[*].bankAccount.issues");
        assertThat(issues).allMatch(issue -> issue == null);

        // spot checks, because the other SEPA-mandates depend on which scenarios ran before
        final List<String> references = response.getFromBody("$[*].reference");
        assertThat(references).contains(ScenarioTest.resolve("%{validMandateReference}", DROP_COMMENTS));
        final List<String> holders = response.getFromBody("$[*].bankAccount.holder");
        assertThat(holders).doesNotContain(
                holder("wrong BIC"), holder("unknown bank code"), holder("expired, wrong BIC"));
    }

    private static String holder(final String holderSuffix) {
        return ScenarioTest.resolve("%{holderPrefix} - " + holderSuffix, DROP_COMMENTS);
    }
}
