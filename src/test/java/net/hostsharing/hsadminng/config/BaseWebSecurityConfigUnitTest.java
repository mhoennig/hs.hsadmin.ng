package net.hostsharing.hsadminng.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.RSA_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseWebSecurityConfigUnitTest {

    private static final String VALID_HMAC_SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String OTHER_HMAC_SECRET =
            "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    private final BaseWebSecurityConfig config = new BaseWebSecurityConfig() {
    };

    private HttpServer httpServer;

    @BeforeEach
    void startHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/", this::handleRequest);
        httpServer.start();
    }

    @AfterEach
    void stopHttpServer() {
        httpServer.stop(0);
    }

    @Test
    void jwtDecoderShouldDecodeRealJwtSignedWithConfiguredHmacSecret() throws Exception {
        val decoder = config.standardJwtDecoder(
                "https://the-issuer.example",
                "https://unused-jwks.example/jwks",
                VALID_HMAC_SECRET,
                "");

        val jwt = decoder.decode(signedJwt("some-subject", VALID_HMAC_SECRET, "https://the-issuer.example", null));

        assertThat(jwt.getSubject()).isEqualTo("some-subject");
        assertThat(jwt.getClaimAsString("given_name")).isEqualTo("Some");
    }

    @Test
    void jwtDecoderShouldRejectRealJwtSignedWithDifferentHmacSecret() throws Exception {
        val decoder = config.standardJwtDecoder("", "", VALID_HMAC_SECRET, "");

        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> decoder.decode(signedJwt("some-subject", OTHER_HMAC_SECRET, null, null)));
    }

    @Test
    void jwtDecoderShouldRejectJwtWithWrongOrMissingIssuer() throws Exception {
        val decoder = config.standardJwtDecoder("https://the-issuer.example", "", VALID_HMAC_SECRET, "");

        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> decoder.decode(signedJwt("some-subject", VALID_HMAC_SECRET, "https://evil-issuer.example", null)));
        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> decoder.decode(signedJwt("some-subject", VALID_HMAC_SECRET, null, null)));
    }

    @Test
    void jwtDecoderShouldAcceptJwtWithAnyOfTheConfiguredAudiences() throws Exception {
        val decoder = config.standardJwtDecoder("", "", VALID_HMAC_SECRET, "first-audience, second-audience");

        val jwt = decoder.decode(signedJwt("some-subject", VALID_HMAC_SECRET, null, "second-audience"));

        assertThat(jwt.getAudience()).containsExactly("second-audience");
    }

    @Test
    void jwtDecoderShouldRejectJwtWithWrongOrMissingAudience() throws Exception {
        val decoder = config.standardJwtDecoder("", "", VALID_HMAC_SECRET, "expected-audience");

        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> decoder.decode(signedJwt("some-subject", VALID_HMAC_SECRET, null, "other-audience")));
        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> decoder.decode(signedJwt("some-subject", VALID_HMAC_SECRET, null, null)));
    }

    @Test
    void jwtDecoderShouldDecodeRealJwtFromConfiguredJwkSetUri() throws Exception {
        val decoder = config.standardJwtDecoder(
                "https://the-issuer.example",
                baseUrl() + "/jwks",
                "",
                "");

        val jwt = decoder.decode(signedRsaJwt("some-subject", "https://the-issuer.example"));

        assertThat(jwt.getSubject()).isEqualTo("some-subject");
        assertThat(jwt.getClaimAsString("given_name")).isEqualTo("Some");
    }

    @Test
    void jwtDecoderShouldRejectJwtFromConfiguredJwkSetUriWithWrongIssuer() throws Exception {
        val decoder = config.standardJwtDecoder(
                "https://the-issuer.example",
                baseUrl() + "/jwks",
                "",
                "");

        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> decoder.decode(signedRsaJwt("some-subject", "https://evil-issuer.example")));
    }

    @Test
    void jwtDecoderShouldDecodeRealJwtFromConfiguredIssuerUri() throws Exception {
        val issuerUri = baseUrl();
        val decoder = config.standardJwtDecoder(issuerUri, "", "", "");

        val jwt = decoder.decode(signedRsaJwt("some-subject", issuerUri));

        assertThat(jwt.getSubject()).isEqualTo("some-subject");
        assertThat(jwt.getIssuer().toString()).isEqualTo(issuerUri);
    }

    @Test
    void jwtDecoderShouldRejectJwtFromConfiguredIssuerUriWithWrongIssuer() throws Exception {
        val decoder = config.standardJwtDecoder(baseUrl(), "", "", "");

        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> decoder.decode(signedRsaJwt("some-subject", "https://evil-issuer.example")));
    }

    @Test
    void jwtDecoderShouldRejectMissingConfiguration() {
        assertThatThrownBy(() -> config.standardJwtDecoder("", "", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Either spring.security.oauth2.resourceserver.jwt.hmac-secret");
    }

    @Test
    void jwtDecoderShouldRejectFakeUrlWithoutProfile() {
        assertThatThrownBy(() -> config.standardJwtDecoder("http://localhost:8080/fake-jwt", "", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You are using a fake-jwt issuer URL")
                .hasMessageContaining("but the 'fake-jwt' profile is not active");
    }

    @Test
    void jwtDecoderShouldRejectFakeJwkSetUriWithoutProfile() {
        assertThatThrownBy(() -> config.standardJwtDecoder("", "http://localhost:8080/fake-jwt/jwks", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You are using a fake-jwt JWK set URI")
                .hasMessageContaining("but the 'fake-jwt' profile is not active");
    }

    private String signedJwt(final String subject, final String secret, final String issuer, final String audience)
            throws Exception {
        val claimsBuilder = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("given_name", "Some")
                .expirationTime(new Date(System.currentTimeMillis() + 60_000));
        if (issuer != null) {
            claimsBuilder.issuer(issuer);
        }
        if (audience != null) {
            claimsBuilder.audience(audience);
        }

        val signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claimsBuilder.build());
        signed.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return signed.serialize();
    }

    private String signedRsaJwt(final String subject, final String issuer) throws Exception {
        val claimsBuilder = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("given_name", "Some")
                .expirationTime(new Date(System.currentTimeMillis() + 60_000));
        if (issuer != null) {
            claimsBuilder.issuer(issuer);
        }

        val signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RSA_KEY.getKeyID()).build(),
                claimsBuilder.build());
        signed.sign(new RSASSASigner(RSA_KEY.toPrivateKey()));
        return signed.serialize();
    }

    private void handleRequest(final HttpExchange exchange) throws IOException {
        switch (exchange.getRequestURI().getPath()) {
            case "/jwks" -> sendJson(exchange, jwksJson());
            case "/.well-known/openid-configuration", "/.well-known/oauth-authorization-server" ->
                sendJson(exchange, issuerConfigurationJson());
            default -> exchange.sendResponseHeaders(404, -1);
        }
    }

    private String issuerConfigurationJson() {
        return """
                {
                  "issuer": "%s",
                  "jwks_uri": "%s/jwks"
                }
                """.formatted(baseUrl(), baseUrl());
    }

    private String jwksJson() {
        return """
                { "keys": [%s] }
                """.formatted(RSA_KEY.toPublicJWK().toJSONString());
    }

    private String baseUrl() {
        return "http://localhost:" + httpServer.getAddress().getPort();
    }

    private void sendJson(final HttpExchange exchange, final String json) throws IOException {
        val response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
