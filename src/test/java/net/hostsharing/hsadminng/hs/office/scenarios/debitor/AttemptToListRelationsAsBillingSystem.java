package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class AttemptToListRelationsAsBillingSystem extends BillingUseCase<AttemptToListRelationsAsBillingSystem> {

    public AttemptToListRelationsAsBillingSystem(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The billing system does not need relations, thus its API-key cannot read any.
                The endpoint-scope `hs.office:billing-mda` rejects the request with `403 Forbidden`,
                although the global ADMIN role of the API_KEY subject would authorize it.
                """ + API_KEY_HINT);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle("Attempt to List Relations", () ->
                httpGet(asBillingSystem(), "/api/hs/office/relations")
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final UseCase<AttemptToListRelationsAsBillingSystem>.HttpResponse response) {
        assertThat(response.getBody()).contains("API-key scopes do not allow GET /api/hs/office/relations");
    }
}
