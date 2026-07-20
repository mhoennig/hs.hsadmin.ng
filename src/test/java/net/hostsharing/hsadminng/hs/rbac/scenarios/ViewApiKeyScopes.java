package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class ViewApiKeyScopes extends UseCase<ViewApiKeyScopes> {

    private final FakeLoginUser loginUser;

    public ViewApiKeyScopes(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                `GET /api/rbac/scopes` lists the available named API-key endpoint-scopes and the
                endpoints each of them allows, e.g. to look up valid values for the `scopes`
                property when creating an API_KEY subject.
                """);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch the available API-key endpoint-scopes", () ->
                httpGet(loginUser, "/api/rbac/scopes")
                        .expecting(OK).expecting(JSON));
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.getBody())
                .contains("rbac.subjects:sync", "*:read")
                .contains("GET /api/rbac/subjects/{uuid}");
    }
}
