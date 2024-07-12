package net.hostsharing.hsadminng.hash;

import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.LINUX_SHA512;
import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.MYSQL_NATIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HashGeneratorUnitTest {

    final String GIVEN_PASSWORD = "given password";
    final String WRONG_PASSWORD = "wrong password";
    final String GIVEN_SALT = "0123456789abcdef";

    // generated via mkpasswd for plaintext password GIVEN_PASSWORD (see above)
    final String GIVEN_LINUX_SHA512_HASH = "$6$ooei1HK6JXVaI7KC$sY5d9fEOr36hjh4CYwIKLMfRKL1539bEmbVCZ.zPiH0sv7jJVnoIXb5YEefEtoSM2WWgDi9hr7vXRe3Nw8zJP/";
    final String GIVEN_LINUX_YESCRYPT_HASH = "$y$j9T$wgYACPmBXvlMg2MzeZA0p1$KXUzd28nG.67GhPnBZ3aZsNNA5bWFdL/dyG4wS0iRw7";

    @Test
    void verifiesLinuxPasswordAgainstSha512HashFromMkpasswd() {
        LinuxEtcShadowHashGenerator.verify(GIVEN_LINUX_SHA512_HASH, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void verifiesLinuxPasswordAgainstYescryptHashFromMkpasswd() {
        LinuxEtcShadowHashGenerator.verify(GIVEN_LINUX_YESCRYPT_HASH, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void verifiesHashedLinuxPasswordWithRandomSalt() {
        final var hash = HashGenerator.using(LINUX_SHA512).withRandomSalt().hash(GIVEN_PASSWORD);
        LinuxEtcShadowHashGenerator.verify(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void verifiesLinuxHashedPasswordWithGivenSalt() {
        final var hash = HashGenerator.using(LINUX_SHA512).withSalt(GIVEN_SALT).hash(GIVEN_PASSWORD);
        LinuxEtcShadowHashGenerator.verify(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void throwsExceptionForInvalidLinuxPassword() {
        final var hash = HashGenerator.using(LINUX_SHA512).withRandomSalt().hash(GIVEN_PASSWORD);
        final var throwable = catchThrowable(() ->
                LinuxEtcShadowHashGenerator.verify(hash, WRONG_PASSWORD)
        );
        assertThat(throwable).hasMessage("invalid password");
    }

    @Test
    void verifiesMySqlNativePassword() {
        final var hash = HashGenerator.using(MYSQL_NATIVE).hash("Test1234");
        assertThat(hash).isEqualTo("*14F1A8C42F8B6D4662BB3ED290FD37BF135FE45C");
    }
}
