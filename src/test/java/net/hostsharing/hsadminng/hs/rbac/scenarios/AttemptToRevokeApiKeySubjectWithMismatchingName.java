package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class AttemptToRevokeApiKeySubjectWithMismatchingName
        extends UseCase<AttemptToRevokeApiKeySubjectWithMismatchingName> {

    private final FakeLoginUser adminLoginUser;

    public AttemptToRevokeApiKeySubjectWithMismatchingName(final ScenarioTest testSuite, final FakeLoginUser adminLoginUser) {
        super(testSuite);
        this.adminLoginUser = adminLoginUser;

        introduction("""
                `DELETE /api/rbac/subjects/{uuid}` has to repeat the subject's name and type as
                safeguard query parameters, which are verified against the subject identified by
                the UUID in the path. If they do not match, nothing is deleted: the request is
                rejected with `400 Bad Request` and the API-key keeps authenticating.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        // prerequisite: an API_KEY subject with the global ADMIN role, created by a global admin
        final var apiKeySubjectCreatedResponse =
                new CreateApiKeySubjectWithGlobalAdminRole(testSuite, adminLoginUser)
                        .thenExpect(CREATED);
        using("apiKey", apiKeySubjectCreatedResponse.getFromBody("apiKey"));

        return withTitle("Attempt to delete the API_KEY subject with a mismatching name", () ->
                httpDelete(adminLoginUser, "/api/rbac/subjects/%{subjectUuid}?name=%{subjectName}.mismatch&type=API_KEY")
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final HttpResponse response) {
        withTitle("The subject still exists", () ->
                httpGet(adminLoginUser, "/api/rbac/subjects/%{subjectUuid}")
                        .expecting(OK).expecting(JSON));
        withTitle("The API-key still authenticates", () ->
                httpGet(withApiKey("%{apiKey}"), "/api/rbac/context")
                        .expecting(OK).expecting(JSON));
    }
}
