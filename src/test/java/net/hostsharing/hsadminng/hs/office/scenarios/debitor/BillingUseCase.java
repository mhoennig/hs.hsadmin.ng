package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.rbac.scenarios.CreateReadOnlyApiKeySubject;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.springframework.http.HttpStatus.CREATED;

abstract class BillingUseCase<T extends UseCase<?>> extends UseCase<T> {

    static final String API_KEY_HINT = """

            The billing system authenticates with a read-only API-key in the `Hostsharing-Api-Key` header.
            Such an API-key has the endpoint-scope `*:read` and the global ADMIN role, see scenario #9690.
            """;

    BillingUseCase(final ScenarioTest testSuite) {
        super(testSuite);
    }

    // the clear-text API-key cannot be kept across scenarios, thus each scenario creates its own, see scenario #9640
    FakeLoginUser asBillingSystem() {
        silently(() -> using(
                "apiKey",
                new CreateReadOnlyApiKeySubject(testSuite, asGlobalAgent()).thenExpect(CREATED).getFromBody("apiKey")));
        return withApiKey("%{apiKey}");
    }
}
