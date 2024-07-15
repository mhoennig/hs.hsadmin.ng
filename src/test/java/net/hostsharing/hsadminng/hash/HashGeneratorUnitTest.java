package net.hostsharing.hsadminng.hash;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.Base64;

import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.LINUX_SHA512;
import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.MYSQL_NATIVE;
import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.SCRAM_SHA256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HashGeneratorUnitTest {

    final String GIVEN_PASSWORD = "given password";
    final String WRONG_PASSWORD = "wrong password";
    final String GIVEN_SALT = "0123456789abcdef";

    // generated via mkpasswd for plaintext password GIVEN_PASSWORD (see above)
    final String GIVEN_LINUX_GENERATED_SHA512_HASH = "$6$ooei1HK6JXVaI7KC$sY5d9fEOr36hjh4CYwIKLMfRKL1539bEmbVCZ.zPiH0sv7jJVnoIXb5YEefEtoSM2WWgDi9hr7vXRe3Nw8zJP/";
    final String GIVEN_LINUX_GENERATED_YESCRYPT_HASH = "$y$j9T$wgYACPmBXvlMg2MzeZA0p1$KXUzd28nG.67GhPnBZ3aZsNNA5bWFdL/dyG4wS0iRw7";

    // generated in PostgreSQL using:
    //  CREATE USER test WITH PASSWORD 'given password';
    //  SELECT rolname, rolpassword FROM pg_authid WHERE rolname = 'test';
    final String GIVEN_POSTGRESQL_GENERATED_SCRAM_SHA256_HASH = "SCRAM-SHA-256$4096:m8M12fdSTsKH+ywthTx1Zw==$4vsB1OddRNdsej9NPAFh91MPdtbOPjkQ85LQZS5lV0Q=:NsVpQNx4Ic/8Sqj1dxfBzUAxyF4FCTMpIsI+bOZCTfA=";

    @Test
    void verifiesLinuxPasswordAgainstSha512HashFromMkpasswd() {
        LinuxEtcShadowHashGenerator.verify(GIVEN_LINUX_GENERATED_SHA512_HASH, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void verifiesLinuxPasswordAgainstYescryptHashFromMkpasswd() {
        LinuxEtcShadowHashGenerator.verify(GIVEN_LINUX_GENERATED_YESCRYPT_HASH, GIVEN_PASSWORD); // throws exception if wrong
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
    void generatesMySqlNativePasswordHash() {
        final var hash = HashGenerator.using(MYSQL_NATIVE).hash("Test1234");
        assertThat(hash).isEqualTo("*14F1A8C42F8B6D4662BB3ED290FD37BF135FE45C");
    }

    @Test
    void generatePostgreSqlScramPasswordHash() {
        // given the same salt, extracted from the hash as generated by PostgreSQL
        final var postgresBase64Salt = Base64.getDecoder().decode(GIVEN_POSTGRESQL_GENERATED_SCRAM_SHA256_HASH.split("\\$")[1].split(":")[1]);

        // when the hash is re-generated via Java
        final var hash = HashGenerator.using(SCRAM_SHA256).withSalt(new String(postgresBase64Salt, Charset.forName("latin1"))).hash(GIVEN_PASSWORD);

        // then we are getting the same hash
        assertThat(hash).isEqualTo(GIVEN_POSTGRESQL_GENERATED_SCRAM_SHA256_HASH);
    }
}
