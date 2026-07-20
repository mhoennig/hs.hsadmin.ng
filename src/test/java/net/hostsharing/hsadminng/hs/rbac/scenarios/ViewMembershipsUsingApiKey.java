package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

public class ViewMembershipsUsingApiKey extends UseCase<ViewMembershipsUsingApiKey> {

    private final FakeLoginUser adminLoginUser;

    public ViewMembershipsUsingApiKey(final ScenarioTest testSuite, final FakeLoginUser adminLoginUser) {
        super(testSuite);
        this.adminLoginUser = adminLoginUser;

        introduction("""
                Any regular business API, here `GET /api/hs/office/memberships` as an example,
                can be used with a valid API-key in the `Hostsharing-Api-Key` header instead of
                a Keycloak OIDC JWT. The API-key acts as its API_KEY subject with whatever roles
                got granted to it, here the global ADMIN role, which can view all memberships.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        // TODO.test: should @Requires the API-key from scenario #9610 instead of creating its own,
        //  but the cross-scenario keep holds only UUIDs, not the once-returned clear-text key
        // prerequisite: an API_KEY subject with the global ADMIN role, created by a global admin
        final var apiKeySubjectCreatedResponse =
                new CreateApiKeySubjectWithGlobalAdminRole(testSuite, adminLoginUser)
                        .thenExpect(CREATED);
        using("apiKey", apiKeySubjectCreatedResponse.getFromBody("apiKey"));

        return withTitle("List arbitrary memberships via the business API, authenticated just by the API-key", () ->
                httpGet(withApiKey("%{apiKey}"), "/api/hs/office/memberships?partnerNumber=P-10001")
                        .expecting(expectedStatus).expecting(JSON));
    }

    @Override
    protected void verify(final HttpResponse response) {
        // global-admin visibility: it sees this membership although its API_KEY subject owns no partner
        assertThat(response.getBody()).contains("M-1000101");
    }
}
