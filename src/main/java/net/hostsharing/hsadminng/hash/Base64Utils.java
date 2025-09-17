package net.hostsharing.hsadminng.hash;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Base64Utils {

    public static boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // Base64 encoding requires at least 2 characters to represent any meaningful data
        if (str.length() < 2) {
            return false;
        }

        if (str.contains("=")) {
            return isValidPaddedBase64(str);
        } else {
            return isValidUnpaddedBase64(str);
        }
    }

    private static boolean isValidPaddedBase64(final String str) {
        // if it has padding, the length must be divisible by 4
        if (str.length() % 4 != 0) {
            return false;
        }
        // check padded Base64 format - padding can only be at the end
        if (!str.matches("^[A-Za-z0-9+/]+={1,2}$")) {
            return false;
        }
        // ensure padding is only at the end
        int paddingStart = str.indexOf('=');
        return paddingStart >= str.length() - 2 && str.substring(paddingStart).matches("^=+$");
    }

    private static boolean isValidUnpaddedBase64(final String str) {
        // check unpadded Base64 (like Argon2 uses)
        // unpadded length should make sense - when padded it should be divisible by 4
        int paddedLength = ((str.length() + 3) / 4) * 4;
        if (paddedLength - str.length() > 2) {
            return false;
        }
        return str.matches("^[A-Za-z0-9+/]+$");
    }
}
