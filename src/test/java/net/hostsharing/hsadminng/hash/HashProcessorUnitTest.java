package net.hostsharing.hsadminng.hash;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static net.hostsharing.hsadminng.hash.HashProcessor.Algorithm.SHA512;
import static net.hostsharing.hsadminng.hash.HashProcessor.hashAlgorithm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HashProcessorUnitTest {

    final String OTHER_PASSWORD = "other password";
    final String GIVEN_PASSWORD = "given password";
    final String GIVEN_PASSWORD_HASH = "foKDNQP0oZo0pjFpss5vNl0kfHOs6MKMaJUUbpJTg6hqI1WY+KbU/PKQIg2xt/mwDMmW5WR0pdUZnTv8RPTfhjprZUNqTXJsUXczQnczYUxE";
    final String GIVEN_SALT = "given salt";

    @Test
    void verifiesHashedPasswordWithRandomSalt() {
        final var hash = hashAlgorithm(SHA512).withRandomSalt().generate(GIVEN_PASSWORD);
        hashAlgorithm(SHA512).withHash(hash).verify(GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void verifiesHashedPasswordWithGivenSalt() {
        final var hash = hashAlgorithm(SHA512).withSalt(GIVEN_SALT).generate(GIVEN_PASSWORD);

        final var decoded = new String(Base64.getDecoder().decode(hash));
        assertThat(decoded).endsWith(":" + GIVEN_SALT);
        hashAlgorithm(SHA512).withHash(hash).verify(GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void throwsExceptionForInvalidPassword() {
        final var throwable = catchThrowable(() ->
            hashAlgorithm(SHA512).withHash(GIVEN_PASSWORD_HASH).verify(OTHER_PASSWORD));

        assertThat(throwable).hasMessage("invalid password");
    }
}
