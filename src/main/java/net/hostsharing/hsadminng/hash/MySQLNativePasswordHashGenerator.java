package net.hostsharing.hsadminng.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MySQLNativePasswordHashGenerator {

    public static String hash(final HashGenerator generator, final String password) {
        // TODO.impl: if a random salt is generated or not should be part of the algorithm definition
//        if (generator.getSalt() != null) {
//            throw new IllegalStateException("salt not supported");
//        }

        try {
            final var sha1 = MessageDigest.getInstance("SHA-1");
            final var firstHash = sha1.digest(password.getBytes());
            final var secondHash = sha1.digest(firstHash);
            return "*" + bytesToHex(secondHash).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        final var hexString = new StringBuilder();
        for (byte b : bytes) {
            final var hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
