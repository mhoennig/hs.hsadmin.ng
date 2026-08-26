package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asAnonymous;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class FetchAuthConfigurationFromForeignOrigin extends UseCase<FetchAuthConfigurationFromForeignOrigin> {

    static final String FOREIGN_ORIGIN = "https://frontend.example";

    public FetchAuthConfigurationFromForeignOrigin(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                A browser-frontend on another origin has to call `GET /api/auth-config` before any login,
                thus without any token (chicken-and-egg), so the endpoint has to answer the CORS-preflight
                and the actual request like the other public `/api` endpoints.
                """);
    }

    @Override
    protected HttpResponse run() {
        withTitle("Answer the CORS-preflight from the foreign origin", () ->
                httpOptions(asAnonymous(), "/api/auth-config", request -> request
                        .header("Origin", FOREIGN_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                        .expecting(OK));
        return withTitle("Fetch the authentication configuration from the foreign origin", () ->
                httpGet(asAnonymous(), "/api/auth-config", request -> request
                        .header("Origin", FOREIGN_ORIGIN))
                        .expecting(OK).expecting(JSON));
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.getResponse().headers().firstValue("Access-Control-Allow-Origin"))
                .as("the response allows the foreign origin").isPresent();
        assertThat(response.<String>getFromBody("method"))
                .as("the foreign origin receives the same configuration").isEqualTo("oidc");
    }
}
