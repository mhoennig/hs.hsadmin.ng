package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import lombok.val;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class ListAllValidSepaMandates extends BillingUseCase<ListAllValidSepaMandates> {

    public ListAllValidSepaMandates(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                With a single request, the billing system fetches all SEPA-mandates which are valid at a key date.
                Each SEPA-mandate contains its debitor, thus `debitor.uuid` or `debitor.debitorNumber` assign it to a debitor.
                """ + API_KEY_HINT);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch All SEPA-Mandates Which Are Valid at the Key Date", () ->
                httpGet(asBillingSystem(), "/api/hs/office/sepamandates?validAt=%{validAt}")
                        .limitReportedResultTo(1)
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final UseCase<ListAllValidSepaMandates>.HttpResponse response) {
        final List<String> references = response.getFromBody("$[*].reference");
        val minimumMandateCount = Integer.parseInt(ScenarioTest.resolve("%{minimumMandateCount}", DROP_COMMENTS));
        assertThat(references).hasSizeGreaterThanOrEqualTo(minimumMandateCount);

        // spot checks, because the other SEPA-mandates depend on which scenarios ran before
        assertThat(references).contains(ScenarioTest.resolve("%{validMandateReference}", DROP_COMMENTS));
        assertThat(references).doesNotContain(ScenarioTest.resolve("%{unexpectedMandateReference}", DROP_COMMENTS));
    }
}
