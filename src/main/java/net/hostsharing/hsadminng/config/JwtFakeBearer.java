package net.hostsharing.hsadminng.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;

import java.util.Date;

/**
 * Provides a fake JWT bearer generator.
 */
public class JwtFakeBearer {

    public static final RSAKey RSA_KEY = generateRSAKey(2048, "test-key");

    @SneakyThrows
    public static String bearer(final String subject) {
        val claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("http://test-issuer")
                .audience("api")
                .expirationTime(new Date(System.currentTimeMillis() + 3600_000))
                .build();

        val signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(RSA_KEY.getKeyID()).build(), claims);
        signed.sign(new RSASSASigner(RSA_KEY.toPrivateKey()));
        return "Bearer " + signed.serialize();
    }

    @SneakyThrows
    private static RSAKey generateRSAKey(final int size, final String keyID) {
        return new RSAKeyGenerator(size).keyID(keyID).generate();
    }
}
