package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.PathAssertion;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class FetchDebitorByDebitorNumber extends BillingUseCase<FetchDebitorByDebitorNumber> {

    public FetchDebitorByDebitorNumber(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                To repeat a billing run for a single debitor, the billing system fetches just this debitor
                by its debitor number, with the same data as an entry of the list of all debitors.
                """ + API_KEY_HINT);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch a Single Debitor by its Debitor Number", () ->
                httpGet(asBillingSystem(), "/api/hs/office/debitors/%{debitorNumber}")
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final UseCase<FetchDebitorByDebitorNumber>.HttpResponse response) {
        new PathAssertion("debitorNumber").contains("%{debitorNumber}").accept(response);
        new PathAssertion("partner.partnerNumber").contains("%{partnerNumber}").accept(response);
        new PathAssertion("debitorRel.contact.postalAddress.city").contains("%{contactCity}").accept(response);
    }
}
