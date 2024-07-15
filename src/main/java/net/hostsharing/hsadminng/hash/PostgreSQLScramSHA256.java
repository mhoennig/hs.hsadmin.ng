package net.hostsharing.hsadminng.hash;

import lombok.SneakyThrows;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class PostgreSQLScramSHA256 {

    private static final String PBKDF_2_WITH_HMAC_SHA256 = "PBKDF2WithHmacSHA256";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256 = "SHA-256";
    private static final int ITERATIONS = 4096;
    public static final int KEY_LENGTH_IN_BITS = 256;

    private static final PostgreSQLScramSHA256 scram = new PostgreSQLScramSHA256();

    @SneakyThrows
    public static String hash(final HashGenerator generator, final String password) {
        if (generator.getSalt() == null) {
            throw new IllegalStateException("no salt given");
        }

        final byte[] salt =  generator.getSalt().getBytes(Charset.forName("latin1")); // Base64.getEncoder().encode(generator.getSalt().getBytes());
        final byte[] saltedPassword = scram.generateSaltedPassword(password, salt);
        final byte[] clientKey = scram.hmacSHA256(saltedPassword, "Client Key".getBytes());
        final byte[] storedKey = MessageDigest.getInstance(SHA256).digest(clientKey);
        final byte[] serverKey = scram.hmacSHA256(saltedPassword, "Server Key".getBytes());

        return "SCRAM-SHA-256${iterations}:{base64EncodedSalt}${base64EncodedStoredKey}:{base64EncodedServerKey}"
                .replace("{iterations}", Integer.toString(ITERATIONS))
                .replace("{base64EncodedSalt}", base64(salt))
                .replace("{base64EncodedStoredKey}", base64(storedKey))
                .replace("{base64EncodedServerKey}", base64(serverKey));
    }

    private static String base64(final byte[] salt) {
        return Base64.getEncoder().encodeToString(salt);
    }

    private byte[] generateSaltedPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_IN_BITS);
        return SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA256).generateSecret(spec).getEncoded();
    }

    private byte[] hmacSHA256(byte[] key, byte[] message)
            throws NoSuchAlgorithmException, InvalidKeyException {
        final var mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(key, HMAC_SHA256));
        return mac.doFinal(message);
    }

}
