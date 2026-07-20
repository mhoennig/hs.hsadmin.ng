package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class RevokeApiKeySubject extends UseCase<RevokeApiKeySubject> {

    private final FakeLoginUser adminLoginUser;

    public RevokeApiKeySubject(final ScenarioTest testSuite, final FakeLoginUser adminLoginUser) {
        super(testSuite);
        this.adminLoginUser = adminLoginUser;

        introduction("""
                An API-key is revoked by permanently deleting its API_KEY subject: `DELETE`
                physically removes the subject together with its grants and its stored API-key
                hash, so the key immediately stops authenticating. There is no soft-delete for
                API_KEY subjects. As a safeguard against deleting the wrong subject, the request
                has to repeat the subject's name and type as query parameters, which are verified
                against the subject identified by the UUID in the path.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        // prerequisite: an API_KEY subject with the global ADMIN role, created by a global admin
        final var apiKeySubjectCreatedResponse =
                new CreateApiKeySubjectWithGlobalAdminRole(testSuite, adminLoginUser)
                        .thenExpect(CREATED);
        using("apiKey", apiKeySubjectCreatedResponse.getFromBody("apiKey"));

        withTitle("Verify the API-key initially authenticates", () ->
                httpGet(withApiKey("%{apiKey}"), "/api/rbac/context")
                        .expecting(OK).expecting(JSON));

        return withTitle("Revoke the API-key by deleting its API_KEY subject", () ->
                httpDelete(adminLoginUser, "/api/rbac/subjects/%{subjectUuid}?name=%{subjectName}&type=API_KEY")
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final HttpResponse response) {
        withTitle("The revoked API-key no longer authenticates", () ->
                httpGet(withApiKey("%{apiKey}"), "/api/rbac/context")
                        .expecting(UNAUTHORIZED));
    }
}
