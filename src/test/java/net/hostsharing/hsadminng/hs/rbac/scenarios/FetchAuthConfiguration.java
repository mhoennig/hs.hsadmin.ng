package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asAnonymous;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class FetchAuthConfiguration extends UseCase<FetchAuthConfiguration> {

    public FetchAuthConfiguration(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                `GET /api/auth-config` reports which authentication method the backend expects and which
                parameters a login for that method needs, so that a frontend can configure its login at
                runtime instead of from environment-specific configuration files distributed separately.
                With the `fake-jwt` profile active, the response reports the token-URL of the local
                password-grant login, which is all a client needs — the fake token endpoint accepts any
                credentials and involves neither a client registration nor any secrets.
                """);
    }

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch the authentication configuration without any login", () ->
                httpGet(asAnonymous(), "/api/auth-config")
                        .expecting(OK).expecting(JSON));
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.<String>getFromBody("method"))
                .as("the backend reports the fake-jwt authentication method").isEqualTo("fake-jwt");
        assertThat(response.<String>getFromBody("tokenUrl"))
                .as("the backend reports the token-URL for the fake-jwt login").isEqualTo("/fake-jwt/token");
    }
}
