package net.hostsharing.hsadminng.config;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Generates and hashes the API-keys of API_KEY subjects.
 *
 * An API-key has the format {@code hsak_<subject-name>.<64 hex chars>}: the embedded subject
 * name makes the key self-describing (like the preferred username in a JWT), the 64 hex chars
 * are 256 random bits. Because the SHA-256 hash of the whole key is stored and looked up on
 * authentication, tampering with the embedded name invalidates the key - no extra signature
 * is needed. The name is informational only and reflects the subject name at creation time.
 *
 * The clear-text API-key is only returned once, in the response of creating the API_KEY subject;
 * just its SHA-256 hash is stored in rbac.api_key.
 */
@UtilityClass
public final class ApiKey {

    public static final String PREFIX = "hsak_"; // "HostSharing Api Key"
    private static final int KEY_BYTES = 32;
    private static final int RANDOM_HEX_CHARS = 2 * KEY_BYTES;
    private static final char NAME_DELIMITER = '.';
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generate(final String subjectName) {
        val randomBytes = new byte[KEY_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return PREFIX + subjectName + NAME_DELIMITER + HexFormat.of().formatHex(randomBytes);
    }

    /** @return the subject name embedded in the given API-key,
     *          or empty if it has no embedded name, e.g. a legacy key {@code hsak_<64 hex chars>} */
    public static Optional<String> subjectNameOf(final String apiKey) {
        // the random part has a fixed length, thus parsing from the end is unambiguous
        // even though subject names may contain the delimiter character
        final var delimiterIndex = apiKey.length() - RANDOM_HEX_CHARS - 1;
        if (!apiKey.startsWith(PREFIX)
                || delimiterIndex <= PREFIX.length()
                || apiKey.charAt(delimiterIndex) != NAME_DELIMITER) {
            return Optional.empty();
        }
        return Optional.of(apiKey.substring(PREFIX.length(), delimiterIndex));
    }

    @SneakyThrows
    public static String hash(final String clearTextApiKey) {
        val digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(clearTextApiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
