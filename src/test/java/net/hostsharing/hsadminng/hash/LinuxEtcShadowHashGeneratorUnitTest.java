package net.hostsharing.hsadminng.hash;

import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.hash.LinuxEtcShadowHashGenerator.Algorithm.SHA512;
import static net.hostsharing.hsadminng.hash.LinuxEtcShadowHashGenerator.hash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LinuxEtcShadowHashGeneratorUnitTest {

    final String GIVEN_PASSWORD = "given password";
    final String WRONG_PASSWORD = "wrong password";
    final String GIVEN_SALT = "0123456789abcdef";

    // generated via mkpasswd for plaintext password GIVEN_PASSWORD (see above)
    final String GIVEN_SHA512_HASH = "$6$ooei1HK6JXVaI7KC$sY5d9fEOr36hjh4CYwIKLMfRKL1539bEmbVCZ.zPiH0sv7jJVnoIXb5YEefEtoSM2WWgDi9hr7vXRe3Nw8zJP/";
    final String GIVEN_YESCRYPT_HASH = "$y$j9T$wgYACPmBXvlMg2MzeZA0p1$KXUzd28nG.67GhPnBZ3aZsNNA5bWFdL/dyG4wS0iRw7";

    @Test
    void verifiesPasswordAgainstSha512HashFromMkpasswd() {
        hash(GIVEN_PASSWORD).verify(GIVEN_SHA512_HASH); // throws exception if wrong
    }

    @Test
    void verifiesPasswordAgainstYescryptHashFromMkpasswd() {
        hash(GIVEN_PASSWORD).verify(GIVEN_YESCRYPT_HASH); // throws exception if wrong
    }

    @Test
    void verifiesHashedPasswordWithRandomSalt() {
        final var hash = hash(GIVEN_PASSWORD).using(SHA512).withRandomSalt().generate();
        hash(GIVEN_PASSWORD).verify(hash); // throws exception if wrong
    }

    @Test
    void verifiesHashedPasswordWithGivenSalt() {
        final var givenPasswordHash =hash(GIVEN_PASSWORD).using(SHA512).withSalt(GIVEN_SALT).generate();
        hash(GIVEN_PASSWORD).verify(givenPasswordHash); // throws exception if wrong
    }

    @Test
    void throwsExceptionForInvalidPassword() {
        final var givenPasswordHash = hash(GIVEN_PASSWORD).using(SHA512).withRandomSalt().generate();

        final var throwable = catchThrowable(() ->
                hash(WRONG_PASSWORD).verify(givenPasswordHash) // throws exception if wrong);
        );
        assertThat(throwable).hasMessage("invalid password");
    }
}
