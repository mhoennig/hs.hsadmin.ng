package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class ListValidSepaMandatesOfDebitor extends BillingUseCase<ListValidSepaMandatesOfDebitor> {

    public ListValidSepaMandatesOfDebitor(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The billing system needs the SEPA-mandate of a debitor which is valid at a key date.
                The query parameter `validAt` excludes expired and future SEPA-mandates.
                The query parameter `debitorUuid` can be used instead of `debitorNumber`.
                """ + API_KEY_HINT);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch the SEPA-Mandates of the Debitor Which Are Valid at the Key Date", () ->
                httpGet(asBillingSystem(), "/api/hs/office/sepamandates?debitorNumber=%{debitorNumber}&validAt=%{validAt}")
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final UseCase<ListValidSepaMandatesOfDebitor>.HttpResponse response) {
        response.expectArrayElements(1);
        path("[0].reference").contains("%{mandateReference}").accept(response);
        path("[0].validFrom").contains("%{mandateValidFrom}").accept(response);
        path("[0].bankAccount.iban").contains("%{bankAccountIBAN}").accept(response);
        path("[0].debitor.debitorNumber").contains("%{debitorNumber}").accept(response);
    }
}
