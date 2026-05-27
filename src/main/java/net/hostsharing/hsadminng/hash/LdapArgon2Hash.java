package net.hostsharing.hsadminng.hash;

import de.mkammerer.argon2.Argon2Factory;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

import static net.hostsharing.hsadminng.hash.Base64Utils.isBase64;

// WARNING: explicit salt and external random salt are silently ignored, a salt gets generated implicitly
@UtilityClass
public class LdapArgon2Hash {

    // align with LDAP config
    public static final int DEFAULT_ITERATIONS = 3;      // t
    public static final int DEFAULT_MEMORY_KIB = 65536;  // m (64 MiB)
    public static final int DEFAULT_PARALLELISM = 1;     // p
    public static final int DEFAULT_SALT_LEN = 16;       // Bytes
    public static final int DEFAULT_HASH_LEN = 32;       // Bytes

    @SneakyThrows
    public static String hash(final HashGenerator generator, final String password) {
        if (isArgon2Hash(password)) {
            return password;
        }
        return hashForLdap(password, DEFAULT_ITERATIONS, DEFAULT_MEMORY_KIB,
                DEFAULT_PARALLELISM, DEFAULT_SALT_LEN, DEFAULT_HASH_LEN);
    }

    public static void verifyHash(final String hash, final String password) {
        // optionally remove prefix to get a pure PHC-String
        final var phc = hash.startsWith("{ARGON2}") ? hash.substring("{ARGON2}".length()) : hash;
        final var argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, DEFAULT_SALT_LEN, DEFAULT_HASH_LEN);
        if (!argon2.verify(phc, password.toCharArray())) {
            throw new IllegalArgumentException("invalid password");
        }
    }

    private static boolean isArgon2Hash(final String password) {
        val hash = password.substring(password.lastIndexOf('$') + 1);
        return password.startsWith("{ARGON2}") && password.contains("$argon2id$")
                && hash.length() > 10 && isBase64(hash);
    }

    public static String hashForLdap(final String password,
            final int iterations,
            final int memoryKiB,
            final int parallelism,
            final int saltLen,
            final int hashLen) {

        final var argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, saltLen, hashLen);
        // generates a PHC-String: $argon2id$v=19$m=..,t=..,p=..$<salt>$<hash>
        final var phc = argon2.hash(iterations, memoryKiB, parallelism, password.toCharArray());
        // but LDAP expects {ARGON2}<PHC-String>
        return "{ARGON2}" + phc;
    }

    public static boolean isValid(final String hash) {
        if (!hash.startsWith("{ARGON2}")) {
            return false;
        }
        final String argon2Part = hash.substring("{ARGON2}".length());
        return argon2Part.contains("$argon2id$") &&
                argon2Part.matches("\\$argon2id\\$v=19\\$m=\\d+,t=\\d+,p=\\d+\\$[A-Za-z0-9+/]{22}\\$[A-Za-z0-9+/]{43}");
    }

}
