package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;

public class CreateReadOnlyApiKeySubject extends UseCase<CreateReadOnlyApiKeySubject> {

    private final FakeLoginUser loginUser;

    public CreateReadOnlyApiKeySubject(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                The endpoint-scope `*:read` makes an API-key read-only: it allows all `GET`
                endpoints under `/api/`, but nothing which changes data. With the global ADMIN
                role granted to its API_KEY subject, such an API-key can read everything,
                e.g. for reporting or monitoring, without any risk of modifying data.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        final var creationResponse = withTitle("Create the read-only API_KEY Subject", () ->
                httpPost(loginUser, "/api/rbac/subjects", usingJsonBody("""
                        {
                            "uuid": ${subjectUuid},
                            "name": ${subjectName},
                            "type": "API_KEY",
                            "scopes": ["*:read"]
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
                "Verify the read-only API-key can use GET endpoints, e.g. list all memberships",
                () -> httpGet(withApiKey("%{apiKey}"), "/api/hs/office/memberships")
                        .limitReportedResultTo(3)
                        .expecting(OK).expecting(JSON),
                currentResponse -> assertThat(currentResponse.getBody()).contains("M-1000101")
        );

        verify(
                "Verify the read-only API-key cannot write, e.g. not upsert a subject",
                () -> httpPut(withApiKey("%{apiKey}"), "/api/rbac/subjects/%{subjectUuid}", usingJsonBody("""
                        {
                            "name": "hsh-never_updated",
                            "type": "USER"
                        }
                        """))
                        .expecting(FORBIDDEN),
                currentResponse -> assertThat(currentResponse.getBody())
                        .contains("API-key scopes do not allow PUT /api/rbac/subjects/")
        );
    }
}
