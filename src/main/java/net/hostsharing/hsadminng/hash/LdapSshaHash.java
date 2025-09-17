package net.hostsharing.hsadminng.hash;

import lombok.SneakyThrows;
import lombok.val;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.hostsharing.hsadminng.hash.Base64Utils.isBase64;

public class LdapSshaHash {

    @SneakyThrows
    public static String hash(final HashGenerator generator, final String password) {
        if (isValid(password)) {
            return password;
        }

        // SSHA is a salted SHA1-1 hash
        val digest =  MessageDigest.getInstance("SHA-1");
        val salt = Optional.ofNullable(generator.getSalt()).orElse("").getBytes(UTF_8);
        digest.update(password.getBytes(UTF_8));
        digest.update(salt);
        val hashBytes = digest.digest();

        // Concatenate hash + salt for SSHA format
        val hashWithSalt = new byte[hashBytes.length + salt.length];
        System.arraycopy(hashBytes, 0, hashWithSalt, 0, hashBytes.length);
        System.arraycopy(salt, 0, hashWithSalt, hashBytes.length, salt.length);

        // Encode to Base64 and add SSHA prefix
        val base64Hash = Base64.getEncoder().encodeToString(hashWithSalt);
        return "{SSHA}" + base64Hash;
    }

    @SneakyThrows
    public static void verifyHash(final String hash, final String password) {
        val digest =  MessageDigest.getInstance("SHA-1");
        val cleanHash = hash.startsWith("{SSHA}") ? hash.substring("{SSHA}".length()) : hash;
        val decodedHash = (isBase64(cleanHash) ? Base64.getDecoder().decode(cleanHash) : cleanHash.getBytes(UTF_8));

        try {
            // SSHA format: first 20 bytes are the SHA-1 hash, remaining bytes are the salt
            if (decodedHash.length <= 20) {
                throw new IllegalArgumentException("Invalid SSHA hash format");
            }

            // Extract the original hash (first 20 bytes)
            val originalHash = new byte[20];
            System.arraycopy(decodedHash, 0, originalHash, 0, 20);

            // Extract the salt (remaining bytes)
            val salt = new byte[decodedHash.length - 20];
            System.arraycopy(decodedHash, 20, salt, 0, salt.length);

            // Standard verification as fallback
            digest.reset();
            digest.update(password.getBytes(UTF_8));
            digest.update(salt);
            val passwordHashBytes = digest.digest();
            
            if (!java.util.Arrays.equals(passwordHashBytes, originalHash)) {
                throw new IllegalArgumentException("invalid password");
            }
        } catch (final Exception e) {
            throw new IllegalArgumentException("invalid password", e);
        }
    }

    public static boolean isValid(final String password) {
        return password.startsWith("{SSHA}") && isBase64(password.substring("{SSHA}".length()));
    }
}
