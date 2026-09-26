package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.rbac.scenarios.CreateBillingApiKeySubject;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.springframework.http.HttpStatus.CREATED;

public abstract class BillingUseCase<T extends UseCase<?>> extends UseCase<T> {

    static final String API_KEY_HINT = """

            The billing system authenticates with an API-key in the `Hostsharing-Api-Key` header.
            Such an API-key has the endpoint-scope `hs.office:billing-mda` and the global ADMIN role, see scenario #3200.
            """;

    BillingUseCase(final ScenarioTest testSuite) {
        super(testSuite);
    }

    public static final String BILLING_MDA_API_KEY = "ApiKey: hsadminng.billing.mda";

    // scenarios which run before scenario #3200 created the billing API-key create their own
    FakeLoginUser asBillingSystem() {
        if (ScenarioTest.knowVariables().containsKey(BILLING_MDA_API_KEY)) {
            return withApiKey("%{" + BILLING_MDA_API_KEY + "}");
        }
        silently(() -> using(
                "apiKey",
                new CreateBillingApiKeySubject(testSuite, asGlobalAgent()).thenExpect(CREATED).getFromBody("apiKey")));
        return withApiKey("%{apiKey}");
    }
}
