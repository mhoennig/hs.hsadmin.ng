package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;

public class InspectBillingApiKeyContext extends BillingUseCase<InspectBillingApiKeyContext> {

    public InspectBillingApiKeyContext(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                Like every API-key, the billing API-key can inspect its own subject, endpoint-scopes and expiry,
                although its endpoint-scope does not list `GET /api/rbac/context`.
                """ + API_KEY_HINT);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle("Inspect the Billing API-Key's Own Properties", () ->
                httpGet(asBillingSystem(), "/api/rbac/context")
                        .expecting(expectedStatus).expecting(JSON));
    }

    @Override
    protected void verify(final UseCase<InspectBillingApiKeyContext>.HttpResponse response) {
        assertThat(response.<String>getFromBody("subject.name"))
                .as("the context reports the API_KEY subject").isEqualTo(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));
        assertThat(response.<Object>getFromBody("apiKey.scopes"))
                .as("the context reports the endpoint-scope").hasToString("[\"hs.office:billing-mda\"]");
    }
}
