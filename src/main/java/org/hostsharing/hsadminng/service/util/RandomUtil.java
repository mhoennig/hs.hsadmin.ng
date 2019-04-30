// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

/**
 * Utility class for generating random Values.
 */
public final class RandomUtil {

    private static final int DEF_COUNT = 20;

    private RandomUtil() {
    }

    /**
     * Generate a password.
     *
     * @return the generated password
     */
    public static String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(DEF_COUNT);
    }

    /**
     * Generate an activation key.
     *
     * @return the generated activation key
     */
    public static String generateActivationKey() {
        return RandomStringUtils.randomNumeric(DEF_COUNT);
    }

    /**
     * Generate a reset key.
     *
     * @return the generated reset key
     */
    public static String generateResetKey() {
        return RandomStringUtils.randomNumeric(DEF_COUNT);
    }

    /**
     * Generate a random enum value for a given enum type.
     *
     * @return the generated enum value
     */
    public static <E extends Enum<E>> E generateEnumValue(final Class<E> enumType) {
        final E[] enumValues = enumType.getEnumConstants();
        return enumValues[RandomUtils.nextInt(0, enumValues.length - 1)];
    }
}
