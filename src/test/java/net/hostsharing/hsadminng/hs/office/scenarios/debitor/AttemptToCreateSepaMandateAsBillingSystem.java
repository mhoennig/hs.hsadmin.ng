package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class AttemptToCreateSepaMandateAsBillingSystem extends BillingUseCase<AttemptToCreateSepaMandateAsBillingSystem> {

    public AttemptToCreateSepaMandateAsBillingSystem(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The billing system only reads SEPA-mandates, thus its API-key cannot create any.
                The endpoint-scope `hs.office:billing-mda` rejects the request with `403 Forbidden`,
                although the global ADMIN role of the API_KEY subject would authorize it.
                """ + API_KEY_HINT);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle("Attempt to Create a SEPA-Mandate", () ->
                httpPost(asBillingSystem(), "/api/hs/office/sepamandates", usingJsonBody("""
                        {
                            "debitor.uuid": ${Debitor: D-3101000 - Test AG - main debitor},
                            "bankAccount.uuid": ${BankAccount: Test AG - debit bank account},
                            "reference": "never created",
                            "agreement": "2026-09-01",
                            "validFrom": "2026-10-01"
                        }
                        """))
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final UseCase<AttemptToCreateSepaMandateAsBillingSystem>.HttpResponse response) {
        assertThat(response.getBody()).contains("API-key scopes do not allow POST /api/hs/office/sepamandates");
    }
}
