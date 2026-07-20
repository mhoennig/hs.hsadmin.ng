package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

public class InspectApiKeyContext extends UseCase<InspectApiKeyContext> {

    private final FakeLoginUser adminLoginUser;

    public InspectApiKeyContext(final ScenarioTest testSuite, final FakeLoginUser adminLoginUser) {
        super(testSuite);
        this.adminLoginUser = adminLoginUser;

        introduction("""
                `GET /api/rbac/context` returns the properties of the API-key used to authenticate
                the request (property `apiKey`): its endpoint-scopes and its expiry timestamp,
                besides the related API_KEY subject itself. This endpoint is always allowed,
                even for endpoint-scoped API-keys, to support such self-inspection.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        // prerequisite: an endpoint-scoped API_KEY subject with an expiry timestamp, created by a global admin
        final var creationResponse = withTitleAndRequestInfo(
                "Create an endpoint-scoped API_KEY Subject with an expiry timestamp",
                """
                        The response contains the generated clear-text API-key (property `apiKey`)
                        exactly once; it cannot be retrieved again.
                        """,
                () -> httpPost(adminLoginUser, "/api/rbac/subjects", usingJsonBody("""
                        {
                            "uuid": ${subjectUuid},
                            "name": ${subjectName},
                            "type": "API_KEY",
                            "scopes": ["rbac.subjects:sync"],
                            "expiresAt": "2030-01-01T00:00:00Z"
                        }
                        """))
                        // deliberately shows the clear-text API-key, it's only valid in the temporary test-database
                        .reportWithResponse()
                        .expecting(CREATED).expecting(JSON));
        using("apiKey", creationResponse.getFromBody("apiKey"));

        return withTitle("Inspect the API-key's own properties, authenticated just by the API-key", () ->
                httpGet(withApiKey("%{apiKey}"), "/api/rbac/context")
                        .expecting(expectedStatus).expecting(JSON));
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.<String>getFromBody("subject.type"))
                .as("the context reports the API_KEY subject").isEqualTo("API_KEY");
        assertThat(response.<Object>getFromBody("apiKey.scopes"))
                .as("the context reports the API-key's endpoint-scopes").hasToString("[\"rbac.subjects:sync\"]");
        assertThat(response.<String>getFromBody("apiKey.expiresAt"))
                .as("the context reports the API-key's expiry timestamp").isEqualTo("2030-01-01T00:00:00Z");
    }
}
