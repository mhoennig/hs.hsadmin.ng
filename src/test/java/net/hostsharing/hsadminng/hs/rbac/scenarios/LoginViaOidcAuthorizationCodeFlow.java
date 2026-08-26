package net.hostsharing.hsadminng.hs.rbac.scenarios;

import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.rbac.scenarios.FetchAuthConfigurationFromForeignOrigin.FOREIGN_ORIGIN;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asAnonymous;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.OK;

public class LoginViaOidcAuthorizationCodeFlow extends UseCase<LoginViaOidcAuthorizationCodeFlow> {

    private static final String CLIENT_ID = "local-frontend";
    private static final String REDIRECT_URI = "http://localhost:3000/auth/callback";
    private static final String PKCE_VERIFIER = "scenario-test-pkce-verifier-0123456789abcdef";

    public LoginViaOidcAuthorizationCodeFlow(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                A browser-frontend on another origin logs in via the standard OIDC authorization-code flow
                (with PKCE) against the authority which `GET /api/auth-config` reported, without any proxy
                in between; therefore `/fake-jwt/**` has to answer CORS requests like the `/api` endpoints.
                The fake provider serves the usual OIDC endpoints: the discovery document, the
                authorization endpoint with its login form, and the token endpoint.
                """);
    }

    @Override
    protected HttpResponse run() {
        withTitle("Fetch the OIDC discovery document from the foreign origin", () ->
                httpGet(asAnonymous(), "/fake-jwt/.well-known/openid-configuration", request -> request
                        .header("Origin", FOREIGN_ORIGIN))
                        .expecting(OK).expecting(JSON));

        // in a browser, the login form under `authorization_endpoint` posts this after the user chose a name
        val authorization = withTitle("Post the login form of the authorization endpoint", () ->
                httpPost(asAnonymous(), "/fake-jwt/auth",
                        usingJsonBody("response_type=code&client_id=" + CLIENT_ID
                                + "&redirect_uri=" + REDIRECT_URI
                                + "&state=some-opaque-state&nonce=some-nonce"
                                + "&code_challenge=" + pkceChallenge(PKCE_VERIFIER)
                                + "&code_challenge_method=S256"
                                + "&username=frontend-developer"),
                        request -> request.setHeader("Content-Type", "application/x-www-form-urlencoded"))
                        .expecting(FOUND));
        val authorizationCode = queryParameter(
                authorization.getResponse().headers().firstValue("Location").orElseThrow(), "code");

        withTitle("Answer the CORS-preflight for the token endpoint from the foreign origin", () ->
                httpOptions(asAnonymous(), "/fake-jwt/token", request -> request
                        .header("Origin", FOREIGN_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                        .expecting(OK));

        return withTitle("Redeem the authorization code at the token endpoint from the foreign origin", () ->
                httpPost(asAnonymous(), "/fake-jwt/token",
                        usingJsonBody("grant_type=authorization_code&client_id=" + CLIENT_ID
                                + "&redirect_uri=" + REDIRECT_URI
                                + "&code=" + authorizationCode
                                + "&code_verifier=" + PKCE_VERIFIER),
                        request -> request
                                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                                .header("Origin", FOREIGN_ORIGIN))
                        .reportWithResponse().expecting(OK).expecting(JSON));
    }

    @Override
    @SneakyThrows
    protected void verify(final HttpResponse response) {
        assertThat(response.getResponse().headers().firstValue("Access-Control-Allow-Origin"))
                .as("the token response allows the foreign origin").isPresent();
        assertThat(response.<String>getFromBody("access_token"))
                .as("the login returns an access token").isNotBlank();

        val idTokenClaims = SignedJWT.parse(response.<String>getFromBody("id_token")).getJWTClaimsSet();
        assertThat(idTokenClaims.getSubject())
                .as("the id_token carries the logged-in user as subject").isEqualTo("frontend-developer");
        assertThat(idTokenClaims.getAudience())
                .as("the id_token is issued for the requesting client").containsExactly(CLIENT_ID);
        assertThat(idTokenClaims.getStringClaim("nonce"))
                .as("the id_token echoes the nonce of the authorization request").isEqualTo("some-nonce");
    }

    @SneakyThrows
    private static String pkceChallenge(final String verifier) {
        val hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private static String queryParameter(final String url, final String name) {
        for (val param : URI.create(url).getQuery().split("&")) {
            if (param.startsWith(name + "=")) {
                return param.substring(name.length() + 1);
            }
        }
        throw new AssertionError("no query parameter '" + name + "' in: " + url);
    }
}
