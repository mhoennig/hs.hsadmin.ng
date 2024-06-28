package net.hostsharing.hsadminng.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import lombok.SneakyThrows;

import jakarta.validation.ValidationException;

import static net.hostsharing.hsadminng.hash.HashProcessor.Algorithm.SHA512;

public class HashProcessor {

    private static final SecureRandom secureRandom = new SecureRandom();

    public enum Algorithm {
        SHA512
    }

    private static final Base64.Encoder BASE64 = Base64.getEncoder();
    private static final String SALT_CHARACTERS =
                    "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "0123456789$_";

    private final MessageDigest generator;
    private byte[] saltBytes;

    @SneakyThrows
    public static HashProcessor hashAlgorithm(final Algorithm algorithm) {
        return new HashProcessor(algorithm);
    }

    private HashProcessor(final Algorithm algorithm) throws NoSuchAlgorithmException {
        generator = MessageDigest.getInstance(algorithm.name());
    }

    public String generate(final String password) {
        final byte[] saltedPasswordDigest = calculateSaltedDigest(password);
        final byte[] hashBytes = appendSaltToSaltedDigest(saltedPasswordDigest);
        return BASE64.encodeToString(hashBytes);
    }

    private byte[] appendSaltToSaltedDigest(final byte[] saltedPasswordDigest) {
        final byte[] hashBytes = new byte[saltedPasswordDigest.length + 1 + saltBytes.length];
        System.arraycopy(saltedPasswordDigest, 0, hashBytes, 0, saltedPasswordDigest.length);
        hashBytes[saltedPasswordDigest.length] = ':';
        System.arraycopy(saltBytes, 0, hashBytes, saltedPasswordDigest.length+1, saltBytes.length);
        return hashBytes;
    }

    private byte[] calculateSaltedDigest(final String password) {
        generator.reset();
        generator.update(password.getBytes());
        generator.update(saltBytes);
        return generator.digest();
    }

    public HashProcessor withSalt(final byte[] saltBytes) {
        this.saltBytes = saltBytes;
        return this;
    }

    public HashProcessor withSalt(final String salt) {
        return withSalt(salt.getBytes());
    }

    public HashProcessor withRandomSalt() {
        final var stringBuilder = new StringBuilder(16);
        for (int i = 0; i < 16; ++i) {
            int randomIndex = secureRandom.nextInt(SALT_CHARACTERS.length());
            stringBuilder.append(SALT_CHARACTERS.charAt(randomIndex));
        }
        return withSalt(stringBuilder.toString());
    }

    public HashVerifier withHash(final String hash) {
        return new HashVerifier(hash);
    }

    private static String getLastPart(String input, char delimiter) {
        final var lastIndex = input.lastIndexOf(delimiter);
        if (lastIndex == -1) {
           throw new IllegalArgumentException("cannot determine salt, expected: 'digest:salt', but no ':' found");
        }
        return input.substring(lastIndex + 1);
    }

    public class HashVerifier {

        private final String hash;

        public HashVerifier(final String hash) {
            this.hash = hash;
            withSalt(getLastPart(new String(Base64.getDecoder().decode(hash)), ':'));
        }

        public void verify(String password) {
            final var computedHash = hashAlgorithm(SHA512).withSalt(saltBytes).generate(password);
            if ( !computedHash.equals(hash) ) {
                throw new ValidationException("invalid password");
            }
        }
    }
}
