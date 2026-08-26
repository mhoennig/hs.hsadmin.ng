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
                The method is always `oidc`: with the `fake-jwt` profile active, the backend itself serves
                a complete OIDC provider under the reported authority, which accepts any credentials and
                involves neither a client registration nor any secrets — a frontend runs the very same
                authorization-code flow as against the production Keycloak.
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
                .as("the backend reports the oidc authentication method").isEqualTo("oidc");
        assertThat(response.<String>getFromBody("authority"))
                .as("the backend reports its own fake OIDC provider as the authority").endsWith("/fake-jwt");
    }
}
