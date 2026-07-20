package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

public class AttemptToCreateSubjectUsingEndpointScopedApiKey
        extends UseCase<AttemptToCreateSubjectUsingEndpointScopedApiKey> {

    private final FakeLoginUser adminLoginUser;

    public AttemptToCreateSubjectUsingEndpointScopedApiKey(
            final ScenarioTest testSuite,
            final FakeLoginUser adminLoginUser) {
        super(testSuite);
        this.adminLoginUser = adminLoginUser;

        introduction("""
                An API-key restricted to the endpoint-scope `rbac.subjects:sync` may only use
                `GET /api/rbac/subjects`, `GET /api/rbac/subjects/{uuid}`, and
                `PUT /api/rbac/subjects/{uuid}`. Even though the global ADMIN role is granted to
                its API_KEY subject, creating subjects via `POST /api/rbac/subjects` is out of scope
                and responds with `403 Forbidden`.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        // prerequisite: an endpoint-scoped API_KEY subject with the global ADMIN role, created by a global admin
        final var apiKeySubjectCreatedResponse =
                new CreateEndpointScopedApiKeySubject(testSuite, adminLoginUser)
                        .thenExpect(CREATED);
        using("apiKey", apiKeySubjectCreatedResponse.getFromBody("apiKey"));

        return withTitle("Attempt to create another Subject, authenticated just by the scoped API-key", () ->
                httpPost(withApiKey("%{apiKey}"), "/api/rbac/subjects", usingJsonBody("""
                        {
                            "name": "never.created.key",
                            "type": "API_KEY"
                        }
                        """))
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.getBody()).contains("API-key scopes do not allow POST /api/rbac/subjects");
    }
}
