package net.hostsharing.hsadminng.hash;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class LinuxEtcShadowHashGenerator {

    public static String hash(final HashGenerator generator, final String payload) {
        if (generator.getSalt() == null) {
            throw new IllegalStateException("no salt given");
        }

        return NativeCryptLibrary.INSTANCE.crypt(payload, "$" + generator.getAlgorithm().enrichedSalt(generator.getSalt()));
    }

    public static void verify(final String givenHash, final String payload) {

        final var parts = givenHash.split("\\$");
        if (parts.length < 3 || parts.length > 5) {
            throw new IllegalArgumentException("hash with unknown hash method: " + givenHash);
        }

        final var algorithm = HashGenerator.Algorithm.byPrefix(parts[1]);
        final var salt = parts.length == 4 ? parts[2] : parts[2] + "$" + parts[3];
        final var calculatedHash = HashGenerator.using(algorithm).withSalt(salt).hash(payload);
        if (!calculatedHash.equals(givenHash)) {
            throw new IllegalArgumentException("invalid password");
        }
    }

    public interface NativeCryptLibrary extends Library {
        NativeCryptLibrary INSTANCE = Native.load("crypt", NativeCryptLibrary.class);

        String crypt(String password, String salt);
    }
}
