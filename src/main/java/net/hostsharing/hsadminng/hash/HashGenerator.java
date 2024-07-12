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

    public enum Algorithm {
        LINUX_SHA512(LinuxEtcShadowHashGenerator::hash, "6"),
        LINUX_YESCRYPT(LinuxEtcShadowHashGenerator::hash, "y"),
        MYSQL_NATIVE(MySQLNativePasswordHashGenerator::hash, "*");

        final BiFunction<HashGenerator, String, String> implementation;
        final String prefix;

        Algorithm(BiFunction<HashGenerator, String, String> implementation, final String prefix) {
            this.implementation = implementation;
            this.prefix = prefix;
        }

        static Algorithm byPrefix(final String prefix) {
            return Arrays.stream(Algorithm.values()).filter(a -> a.prefix.equals(prefix)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("unknown hash algorithm: '" + prefix + "'"));
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

    public String hash(final String plaintextPassword) {
        if (plaintextPassword == null) {
            throw new IllegalStateException("no password given");
        }

        return algorithm.implementation.apply(this, plaintextPassword);
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
