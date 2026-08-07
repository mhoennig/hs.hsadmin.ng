package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.rbac.scenarios.FetchAuthConfigurationFromForeignOrigin.FOREIGN_ORIGIN;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asAnonymous;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class LoginAtReportedFakeJwtTokenUrl extends UseCase<LoginAtReportedFakeJwtTokenUrl> {

    public LoginAtReportedFakeJwtTokenUrl(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                A browser-frontend on another origin logs in at the token-URL which
                `GET /api/auth-config` reported, without any proxy in between; therefore
                `/fake-jwt/**` has to answer CORS requests like the `/api` endpoints do.
                """);
    }

    @Override
    protected HttpResponse run() {
        final var authConfig = withTitle("Fetch the authentication configuration without any login", () ->
                httpGet(asAnonymous(), "/api/auth-config")
                        .expecting(OK).expecting(JSON));
        final String tokenUrl = authConfig.getFromBody("tokenUrl");

        withTitle("Answer the CORS-preflight for the token-URL from the foreign origin", () ->
                httpOptions(asAnonymous(), tokenUrl, request -> request
                        .header("Origin", FOREIGN_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                        .expecting(OK));

        return withTitle("Log in at the reported token-URL directly from the foreign origin", () ->
                httpPost(asAnonymous(), tokenUrl,
                        usingJsonBody("username=frontend-developer&password=anything"),
                        request -> request
                                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                                .header("Origin", FOREIGN_ORIGIN))
                        .expecting(OK).expecting(JSON));
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.getResponse().headers().firstValue("Access-Control-Allow-Origin"))
                .as("the token response allows the foreign origin").isPresent();
        assertThat(response.<String>getFromBody("access_token"))
                .as("the login returns an access token").isNotBlank();
    }
}
