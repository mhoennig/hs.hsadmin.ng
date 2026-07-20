package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class CreateEndpointScopedApiKeySubject extends UseCase<CreateEndpointScopedApiKeySubject> {

    private final FakeLoginUser loginUser;

    public CreateEndpointScopedApiKeySubject(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                An API-key can be restricted to named endpoint-scopes, here `rbac.subjects:sync`,
                which only allows `GET /api/rbac/subjects`, `GET /api/rbac/subjects/{uuid}`,
                and `PUT /api/rbac/subjects/{uuid}` (create-or-update, incl. declarative
                deactivation via `deactivated: true` — deliberately no `DELETE`).
                The scopes are an additional fence on top of the roles granted to the API_KEY
                subject: even with the global ADMIN role, e.g. granted to sync ALL subjects as
                needed for a Keycloak subject synchronization, all endpoints outside the scopes
                respond with `403 Forbidden`. An API-key without scopes remains unrestricted.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        final var creationResponse = withTitleAndRequestInfo(
                "Create the endpoint-scoped API_KEY Subject",
                """
                        The `scopes` property restricts the API-key to the given named endpoint-scopes.
                        The response contains the generated clear-text API-key (property `apiKey`)
                        exactly once; it cannot be retrieved again.
                        """,
                () -> httpPost(loginUser, "/api/rbac/subjects", usingJsonBody("""
                        {
                            "uuid": ${subjectUuid},
                            "name": ${subjectName},
                            "type": "API_KEY",
                            "scopes": ["rbac.subjects:sync"]
                        }
                        """))
                        // deliberately shows the clear-text API-key, it's only valid in the temporary test-database
                        .reportWithResponse()
                        .expecting(expectedStatus));
        if (creationResponse.getStatus() != CREATED) {
            return creationResponse;
        }
        using("apiKey", creationResponse.getFromBody("apiKey"));

        withTitleAndRequestInfo(
                "Prerequisite: Resolve the UUID of the global ADMIN role",
                """
                        The grant API needs the UUID of the role which we want to grant.
                        """,
                () -> httpGet(loginUser, "/api/rbac/roles?name=" + uriEncoded("rbac.global#global:ADMIN"))
                        .expecting(OK).expecting(JSON).expectArrayElements(1)
                        .extractUuidAlias("[0].uuid", "globalAdminRoleUuidToGrant")
        );

        withTitle("Grant the global ADMIN role to the API_KEY Subject", () ->
                httpPost(loginUser, "/api/rbac/grants", usingJsonBody("""
                        {
                          "assumed": true,
                          "grantedRole.uuid": "%{globalAdminRoleUuidToGrant}",
                          "granteeSubject.uuid": "%{subjectUuid}"
                        }
                        """),
                        request -> request.header("Hostsharing-Assumed-Roles", "rbac.global#global:ADMIN"))
                        .expecting(CREATED).expecting(JSON)
        );

        return creationResponse;
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.<Object>getFromBody("scopes"))
                .as("the created API-key echoes its scopes").hasToString("[\"rbac.subjects:sync\"]");

        verify(
                "Verify the API-key can list the RBAC subjects of ALL realms via its global-admin role",
                () -> httpGet(withApiKey("%{apiKey}"), "/api/rbac/subjects")
                        .limitReportedResultTo(5)
                        .expecting(OK).expecting(JSON),
                // a realm-less API_KEY subject only sees foreign-realm subjects via the global ADMIN role
                currentResponse -> assertThat(currentResponse.getBody()).contains("hsh-alex_superuser")
        );

        verify(
                "Verify the API-key can fetch a single RBAC subject by its UUID",
                () -> httpGet(withApiKey("%{apiKey}"), "/api/rbac/subjects/%{subjectUuid}")
                        .expecting(OK).expecting(JSON),
                path("name").contains("%{subjectName}"),
                path("type").contains("API_KEY")
        );
    }
}
