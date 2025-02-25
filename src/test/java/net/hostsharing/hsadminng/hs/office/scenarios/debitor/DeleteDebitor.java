package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

public class DeleteDebitor extends UseCase<DeleteDebitor> {

    public DeleteDebitor(final ScenarioTest testSuite) {
        super(testSuite);

        requires("Debitor: Test AG - delete debitor", alias -> new CreateSelfDebitorForPartner(testSuite)
                .given("partnerPersonTradeName", "Test AG")
                .given("billingContactCaption", "Test AG - billing department")
                .given("billingContactEmailAddress", "billing@test-ag.example.org")
                .given("debitorNumberSuffix", "%{debitorSuffix}")
                .given("billable", true)
                .given("vatId", "VAT123456")
                .given("vatCountryCode", "DE")
                .given("vatBusiness", true)
                .given("vatReverseCharge", false)
                .given("defaultPrefix", "tsz"));
    }

    @Override
    protected HttpResponse run() {
        withTitle("Delete the Debitor using its UUID", () ->
            httpDelete("/api/hs/office/debitors/&{Debitor: Test AG - delete debitor}")
                .expecting(HttpStatus.NO_CONTENT)
        );
        return null;
    }
}
