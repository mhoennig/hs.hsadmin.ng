package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class AttemptToCreateDebitorAsBillingSystem extends BillingUseCase<AttemptToCreateDebitorAsBillingSystem> {

    public AttemptToCreateDebitorAsBillingSystem(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The billing system only reads debitors, thus its API-key cannot create any.
                The endpoint-scope `hs.office:billing-mda` rejects the request with `403 Forbidden`,
                although the global ADMIN role of the API_KEY subject would authorize it.
                """ + API_KEY_HINT);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle("Attempt to Create a Debitor", () ->
                httpPost(asBillingSystem(), "/api/hs/office/debitors", usingJsonBody("""
                        {
                            "debitorRel": {
                                "anchor.uuid": ${Person: Test AG},
                                "holder.uuid": ${Person: Test AG},
                                "contact.uuid": ${Contact: Test AG - Hamburg}
                            },
                            "debitorNumberSuffix": "99",
                            "billable": true,
                            "vatBusiness": false,
                            "defaultPrefix": "mix"
                        }
                        """))
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final UseCase<AttemptToCreateDebitorAsBillingSystem>.HttpResponse response) {
        assertThat(response.getBody()).contains("API-key scopes do not allow POST /api/hs/office/debitors");
    }
}
