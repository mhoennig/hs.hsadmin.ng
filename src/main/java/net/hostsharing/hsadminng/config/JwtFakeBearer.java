package net.hostsharing.hsadminng.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.List;

/**
 * Provides a fake JWT bearer generator.
 */
public class JwtFakeBearer {

    public static final RSAKey RSA_KEY = generateRSAKey(2048, "test-key");
    public static final int ACCESS_TOKEN_EXPIRES_IN_SECONDS = 3600;
    public static final int REFRESH_TOKEN_EXPIRES_IN_SECONDS = 72 * 3600;

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @SneakyThrows
    public static String bearer(final String subject) {
        return bearer(subject, List.of());
    }

    @SneakyThrows
    public static String bearer(final String subject, final Collection<String> groups) {
        return "Bearer " + accessToken(subject, groups);
    }

    @SneakyThrows
    public static String accessToken(final String subject, final Collection<String> groups) {
        val claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("http://test-issuer")
                .audience("api")
                .claim("groups", groups)
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(expiresIn(ACCESS_TOKEN_EXPIRES_IN_SECONDS))
                .build();

        return sign(claims);
    }

    /** Fakes a bearer with the same claims as the JWT synthesized by the ApiKeyAuthenticationFilter,
     * e.g. for RestTests of controllers which inspect API-key authentications. */
    @SneakyThrows
    public static String apiKeyBearer(
            final String subjectUuid, final Collection<String> scopes, final Instant apiKeyExpiresAt) {
        val claimsBuilder = new JWTClaimsSet.Builder()
                .subject(subjectUuid)
                .issuer("http://test-issuer")
                .audience("api")
                .claim(ApiKeyAuthenticationFilter.TOKEN_TYPE_CLAIM, ApiKeyAuthenticationFilter.TOKEN_TYPE_API_KEY)
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(expiresIn(ACCESS_TOKEN_EXPIRES_IN_SECONDS));
        if (!scopes.isEmpty()) {
            claimsBuilder.claim(ApiKeyAuthenticationFilter.SCOPE_CLAIM, String.join(" ", scopes));
        }
        if (apiKeyExpiresAt != null) {
            claimsBuilder.claim(ApiKeyAuthenticationFilter.API_KEY_EXPIRES_AT_CLAIM, apiKeyExpiresAt.getEpochSecond());
        }
        return "Bearer " + sign(claimsBuilder.build());
    }

    @SneakyThrows
    public static String refreshToken(final String subject) {
        val claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("http://test-issuer")
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(expiresIn(REFRESH_TOKEN_EXPIRES_IN_SECONDS))
                .build();

        return sign(claims);
    }

    @SneakyThrows
    public static String subjectFromRefreshToken(final String refreshToken) {
        val signed = SignedJWT.parse(refreshToken);
        if (!signed.verify(new RSASSAVerifier(RSA_KEY.toRSAPublicKey()))) {
            throw new IllegalArgumentException("invalid refresh token signature");
        }
        val claims = signed.getJWTClaimsSet();
        if (!REFRESH_TOKEN_TYPE.equals(claims.getStringClaim(TOKEN_TYPE_CLAIM))) {
            throw new IllegalArgumentException("not a refresh token");
        }
        if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
            throw new IllegalArgumentException("refresh token expired");
        }
        return claims.getSubject();
    }

    @SneakyThrows
    private static String sign(final JWTClaimsSet claims) {
        val signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(RSA_KEY.getKeyID()).build(), claims);
        signed.sign(new RSASSASigner(RSA_KEY.toPrivateKey()));
        return signed.serialize();
    }

    private static Date expiresIn(final int seconds) {
        return new Date(System.currentTimeMillis() + seconds * 1000L);
    }

    @SneakyThrows
    private static RSAKey generateRSAKey(final int size, final String keyID) {
        return new RSAKeyGenerator(size).keyID(keyID).generate();
    }
}
