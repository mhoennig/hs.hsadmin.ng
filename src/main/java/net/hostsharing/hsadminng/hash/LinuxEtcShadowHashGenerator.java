package net.hostsharing.hsadminng.hash;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.random.RandomGenerator;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class LinuxEtcShadowHashGenerator {

    private static final RandomGenerator random = new SecureRandom();
    private static final Queue<String> predefinedSalts = new PriorityQueue<>();

    public static final int SALT_LENGTH = 16;

    private final String plaintextPassword;
    private Algorithm algorithm;

    public enum Algorithm {
        SHA512("6"),
        YESCRYPT("y");

        final String prefix;

        Algorithm(final String prefix) {
            this.prefix = prefix;
        }

        static Algorithm byPrefix(final String prefix) {
            return Arrays.stream(Algorithm.values()).filter(a -> a.prefix.equals(prefix)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("unknown hash algorithm: '" + prefix + "'"));
        }
    }

    private static final String SALT_CHARACTERS =
                    "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "0123456789/.";

    private String salt;

    public static LinuxEtcShadowHashGenerator hash(final String plaintextPassword) {
        return new LinuxEtcShadowHashGenerator(plaintextPassword);
    }

    private LinuxEtcShadowHashGenerator(final String plaintextPassword) {
       this.plaintextPassword = plaintextPassword;
    }

    public LinuxEtcShadowHashGenerator using(final Algorithm algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    void verify(final String givenHash) {
        final var parts = givenHash.split("\\$");
        if (parts.length < 3 || parts.length > 5) {
            throw new IllegalArgumentException("not a " + algorithm.name() + " Linux hash: " + givenHash);
        }

        algorithm = Algorithm.byPrefix(parts[1]);
        salt = parts.length == 4 ? parts[2] : parts[2] + "$" + parts[3];

        if (!generate().equals(givenHash)) {
            throw new IllegalArgumentException("invalid password");
        }
    }

    public String generate() {
        if (salt == null) {
            throw new IllegalStateException("no salt given");
        }
        if (plaintextPassword == null) {
            throw new IllegalStateException("no password given");
        }

        return NativeCryptLibrary.INSTANCE.crypt(plaintextPassword, "$" + algorithm.prefix + "$" + salt);
    }

    public static void nextSalt(final String salt) {
        predefinedSalts.add(salt);
    }

    public LinuxEtcShadowHashGenerator withSalt(final String salt) {
        this.salt = salt;
        return this;
    }

    public LinuxEtcShadowHashGenerator withRandomSalt() {
        if (!predefinedSalts.isEmpty()) {
            return withSalt(predefinedSalts.poll());
        }
        final var stringBuilder = new StringBuilder(SALT_LENGTH);
        for (int i = 0; i < SALT_LENGTH; ++i) {
            int randomIndex = random.nextInt(SALT_CHARACTERS.length());
            stringBuilder.append(SALT_CHARACTERS.charAt(randomIndex));
        }
        return withSalt(stringBuilder.toString());
    }
    public static void main(String[] args) {
        System.out.println(NativeCryptLibrary.INSTANCE.crypt("given password", "$6$abcdefghijklmno"));
    }

    public interface NativeCryptLibrary extends Library {
        NativeCryptLibrary INSTANCE = Native.load("crypt", NativeCryptLibrary.class);

        String crypt(String password, String salt);
    }
}
