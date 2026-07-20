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

public class CreateApiKeySubjectWithGlobalAdminRole extends UseCase<CreateApiKeySubjectWithGlobalAdminRole> {

    private final FakeLoginUser loginUser;

    public CreateApiKeySubjectWithGlobalAdminRole(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                API_KEY subjects authenticate technical clients via the `Hostsharing-Api-Key` HTTP header
                instead of a Keycloak OIDC JWT, e.g. automation programs, completely bypassing Keycloak.
                Only a global-admin may create API_KEY subjects. The clear-text API-key is returned only
                once, in the response of creating the API_KEY subject; just its hash gets stored.
                Like GROUP subjects, API_KEY subjects cannot have an account. Global API_KEY subjects
                do not belong to a realm, thus their name must neither contain a `-` (the realm-prefix
                delimiter) nor a `/` (the GROUP subject marker).
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        final var creationResponse = withTitleAndRequestInfo(
                "Create the API_KEY Subject",
                """
                        The response contains the generated clear-text API-key (property `apiKey`)
                        exactly once; it cannot be retrieved again.
                        """,
                () -> httpPost(loginUser, "/api/rbac/subjects", usingJsonBody("""
                        {
                            "uuid": ${subjectUuid},
                            "name": ${subjectName},
                            "type": "API_KEY"
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
        verify(
                "Verify the API-key authenticates as its Subject with the global-admin role, without any JWT",
                () -> httpGet(withApiKey("%{apiKey}"), "/api/hs/accounts/current")
                        .expecting(OK).expecting(JSON),
                path("subject.name").contains("%{subjectName}"),
                path("subject.type").contains("API_KEY"),
                path("globalAdmin").contains("true"),
                currentResponse -> assertThat(currentResponse.<Object>getFromBody("person"))
                        .as("API_KEY subjects cannot have an account, thus no person").isNull()
        );
    }
}
