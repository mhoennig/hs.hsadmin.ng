package net.hostsharing.hsadminng.hash;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

import lombok.Getter;

/**
 * Usage-example to generate hash:
 *  HashGenerator.using(LINUX_SHA512).withRandomSalt().hash("plaintext password");
 *
 * Usage-example to verify hash:
 *  HashGenerator.fromHash("hashed password).verify("plaintext password");
 */
@Getter
public final class HashGenerator {

    private static final RandomGenerator random = new SecureRandom();
    private static final Queue<String> predefinedSalts = new PriorityQueue<>();

    public static final int RANDOM_SALT_LENGTH = 16;
    private static final String RANDOM_SALT_CHARACTERS =
            "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "0123456789/.";
    private static boolean couldBeHashEnabled; // TODO.legacy: remove after legacy data is migrated

    public enum Algorithm {
        LINUX_SHA512(LinuxEtcShadowHashGenerator::hash, "6"),
        LINUX_YESCRYPT(LinuxEtcShadowHashGenerator::hash, "y", "j9T$") {
            @Override
            String enrichedSalt(final String salt) {
                return prefix + "$" + (salt.startsWith(optionalParam) ? salt : optionalParam + salt);
            }
        },
        MYSQL_NATIVE(MySQLNativePasswordHashGenerator::hash, "*"),
        SCRAM_SHA256(PostgreSQLScramSHA256::hash, "SCRAM-SHA-256");

        final BiFunction<HashGenerator, String, String> implementation;
        final String prefix;
        final String optionalParam;

        Algorithm(BiFunction<HashGenerator, String, String> implementation, final String prefix, final String optionalParam) {
            this.implementation = implementation;
            this.prefix = prefix;
            this.optionalParam = optionalParam;
        }

        Algorithm(BiFunction<HashGenerator, String, String> implementation, final String prefix) {
            this(implementation, prefix, null);
        }

        static Algorithm byPrefix(final String prefix) {
            return Arrays.stream(Algorithm.values()).filter(a -> a.prefix.equals(prefix)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("unknown hash algorithm: '" + prefix + "'"));
        }

        String enrichedSalt(final String salt) {
            return prefix + "$" + salt;
        }
    }

    private final Algorithm algorithm;
    private String salt;

    public static HashGenerator using(final Algorithm algorithm) {
        return new HashGenerator(algorithm);
    }

    private HashGenerator(final Algorithm algorithm) {
       this.algorithm = algorithm;
    }

    public static void enableCouldBeHash(final boolean enable) {
        couldBeHashEnabled = enable;
    }

    public boolean couldBeHash(final String value) {
        return couldBeHashEnabled && value.startsWith(algorithm.prefix);
    }

    public String hash(final String plaintextPassword) {
        if (plaintextPassword == null) {
            throw new IllegalStateException("no password given");
        }

        final var hash = algorithm.implementation.apply(this, plaintextPassword);
        if (hash.length() < plaintextPassword.length()) {
            throw new AssertionError("generated hash too short: " + hash);
        }
        return hash;
    }

    public String hashIfNotYetHashed(final String plaintextPasswordOrHash) {
        return couldBeHash(plaintextPasswordOrHash)
                ? plaintextPasswordOrHash
                : hash(plaintextPasswordOrHash);
    }

    public static void nextSalt(final String salt) {
        predefinedSalts.add(salt);
    }

    public HashGenerator withSalt(final String salt) {
        this.salt = salt;
        return this;
    }

    public HashGenerator withRandomSalt() {
        if (!predefinedSalts.isEmpty()) {
            return withSalt(predefinedSalts.poll());
        }
        final var stringBuilder = new StringBuilder(RANDOM_SALT_LENGTH);
        for (int i = 0; i < RANDOM_SALT_LENGTH; ++i) {
            int randomIndex = random.nextInt(RANDOM_SALT_CHARACTERS.length());
            stringBuilder.append(RANDOM_SALT_CHARACTERS.charAt(randomIndex));
        }
        return withSalt(stringBuilder.toString());
    }
}
