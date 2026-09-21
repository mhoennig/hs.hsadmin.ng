package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import lombok.val;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.office.scenarios.debitor.ListDebitorsWithCompleteMasterData.HINTS_FOR_BILLING;
import static net.hostsharing.hsadminng.hs.office.scenarios.debitor.ListDebitorsWithCompleteMasterData.verifyDebitorAt;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class ListAllDebitorsWithCompleteMasterData extends UseCase<ListAllDebitorsWithCompleteMasterData> {

    public ListAllDebitorsWithCompleteMasterData(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The billing system fetches all debitors with a single request.
                Each debitor contains the complete debitor number, the partner person and the contact.
                """ + HINTS_FOR_BILLING);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch the List of All Debitors", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/debitors")
                        .limitReportedResultTo(1)
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final UseCase<ListAllDebitorsWithCompleteMasterData>.HttpResponse response) {
        final List<String> debitorNumbers = response.getFromBody("$[*].debitorNumber");
        val minimumDebitorCount = Integer.parseInt(ScenarioTest.resolve("%{minimumDebitorCount}", DROP_COMMENTS));
        assertThat(debitorNumbers).hasSizeGreaterThanOrEqualTo(minimumDebitorCount);

        // a spot check, because the other debitors depend on which scenarios ran before
        val sampleIndex = debitorNumbers.indexOf(ScenarioTest.resolve("%{debitorNumber}", DROP_COMMENTS));
        assertThat(sampleIndex).as("the sample debitor is missing in the list").isNotNegative();
        verifyDebitorAt(response, sampleIndex);
    }
}
