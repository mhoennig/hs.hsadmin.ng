package net.hostsharing.hsadminng.hash;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.util.Base64;

import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.LDAP_ARGON2;
import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.LDAP_SSHA;
import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.LINUX_SHA512;
import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.LINUX_YESCRYPT;
import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.MYSQL_NATIVE;
import static net.hostsharing.hsadminng.hash.HashGenerator.Algorithm.SCRAM_SHA256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class HashGeneratorUnitTest {

    static final int SHA1_DIGEST_LENGTH_BYTES = 20;
    static final int HEX_DIGITS_PER_BYTE = 2;
    static final int MYSQL_NATIVE_HASH_LENGTH = "*".length() + SHA1_DIGEST_LENGTH_BYTES * HEX_DIGITS_PER_BYTE;

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
    void fetchesHashGeneratorFromEnvVarDefault() {
        {
            val hash = HashGenerator.fromEnv("NON_EXISTING_ENV_VAR", "{SSHA}").withRandomSalt().hash(GIVEN_PASSWORD);
            LdapSshaHash.verifyHash(hash, GIVEN_PASSWORD); // throws exception if wrong
        }

        {
            val hash = HashGenerator.fromEnv("NON_EXISTING_ENV_VAR", "{ARGON2}").withRandomSalt().hash(GIVEN_PASSWORD);
            LdapArgon2Hash.verifyHash(hash, GIVEN_PASSWORD); // throws exception if wrong
        }
    }

    @Test
    void returnsNullHashForNullPassword() {
        assertThat(HashGenerator.using(LDAP_ARGON2).hash(null)).isNull();
    }

    @Test
    void rejectsGeneratedHashShorterThanPlaintextPassword() {
        val plaintextLongerThanGeneratedMySqlNativeHash = "x".repeat(MYSQL_NATIVE_HASH_LENGTH + 1);

        val throwable = catchThrowable(() ->
                HashGenerator.using(MYSQL_NATIVE).hash(plaintextLongerThanGeneratedMySqlNativeHash)
        );

        assertThat(throwable).isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("generated hash too short: *");
    }

    @Test
    void hashesPasswordIfNotYetHashed() {
        HashGenerator.enableCouldBeHash(false);

        val hash = HashGenerator.using(LDAP_SSHA)
                .withSalt(GIVEN_SALT)
                .hashIfNotYetHashed(GIVEN_PASSWORD);

        LdapSshaHash.verifyHash(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void hashesPasswordIfHashDetectionIsEnabledButValueDoesNotMatchAlgorithmPrefix() {
        HashGenerator.enableCouldBeHash(true);
        try {
            val hash = HashGenerator.using(LDAP_SSHA)
                    .withSalt(GIVEN_SALT)
                    .hashIfNotYetHashed(GIVEN_PASSWORD);

            LdapSshaHash.verifyHash(hash, GIVEN_PASSWORD); // throws exception if wrong
        } finally {
            HashGenerator.enableCouldBeHash(false);
        }
    }

    @Test
    void avoidsHashingPasswordIfHashDetectionIsEnabledAndValueCouldBeHash() {
        HashGenerator.enableCouldBeHash(true);
        try {
            val hashedPassword = "{SSHA}not-validated-here";

            val hash = HashGenerator.using(LDAP_SSHA).hashIfNotYetHashed(hashedPassword);

            assertThat(hash).isEqualTo(hashedPassword);
        } finally {
            HashGenerator.enableCouldBeHash(false);
        }
    }

    @Test
    void verifiesPasswordAgainstGeneratedArgon2Hash() {
        val hash = HashGenerator.using(LDAP_ARGON2).withSalt(null).hash(GIVEN_PASSWORD);
        LdapArgon2Hash.verifyHash(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void verifiesPasswordAgainstGeneratedArgon2PhcStringWithoutLdapPrefix() {
        val hash = HashGenerator.using(LDAP_ARGON2).withSalt(null).hash(GIVEN_PASSWORD);
        val phc = hash.substring("{ARGON2}".length());

        LdapArgon2Hash.verifyHash(phc, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void rejectsInvalidPasswordAgainstGeneratedArgon2Hash() {
        val hash = HashGenerator.using(LDAP_ARGON2).withSalt(null).hash(GIVEN_PASSWORD);
        val throwable = catchThrowable(() ->
                LdapArgon2Hash.verifyHash(hash, GIVEN_PASSWORD+"x") // throws exception if wrong
        );
        assertThat(throwable).hasMessage("invalid password");
    }

    @Test
    void rejectsNullArgon2Password() {
        val throwable = catchThrowable(() ->
                LdapArgon2Hash.hash(null, null)
        );
        assertThat(throwable).isInstanceOf(NullPointerException.class);
    }

    @Test
    void currentArgon2AdapterIgnoresExplicitSalt() {
        val hash = HashGenerator.using(LDAP_ARGON2).withRandomSalt().hash(GIVEN_PASSWORD);
        LdapArgon2Hash.verifyHash(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void enrichesArgon2SaltWithLdapPrefixOnly() {
        assertThat(LDAP_ARGON2.enrichedSalt("$argon2id$v=19$m=1024,t=2,p=2$given-salt"))
                .isEqualTo("{ARGON2}$argon2id$v=19$m=1024,t=2,p=2$given-salt");
    }

    @Test
    void avoidsDoubleHashingArgon2AHashPassword() {
        val hashedPassword = "{ARGON2}$argon2id$v=19$m=65536,t=3,p=1$pEabRksh7EJQV+OwPR5n7Q$83qQtZe2J8+fteWm7g/uvXksfhJKGsipZFsuAaJtBjs";
        val hash = HashGenerator.using(LDAP_ARGON2).hash(hashedPassword);
        assertThat(hash).isEqualTo(hashedPassword);
    }

    @Test
    void hashesPasswordWhichLooksLikeArgon2AHashButIsNot() {
        val password = "{ARGON2}$argon2id$das-ist-kein-base64-hash";
        val hash = HashGenerator.using(LDAP_ARGON2).hash(password);
        LdapArgon2Hash.verifyHash(hash, password); // throws exception if wrong
    }

    @Test
    void hashesPasswordWhichStartsWithArgon2PrefixButDoesNotContainArgon2IdMarker() {
        val password = "{ARGON2}not-an-argon2-hash";
        val hash = HashGenerator.using(LDAP_ARGON2).hash(password);
        LdapArgon2Hash.verifyHash(hash, password); // throws exception if wrong
    }

    @Test
    void hashesPasswordWhichLooksLikeArgon2HashButHasTooShortHashValue() {
        val password = "{ARGON2}$argon2id$short";
        val hash = HashGenerator.using(LDAP_ARGON2).hash(password);
        LdapArgon2Hash.verifyHash(hash, password); // throws exception if wrong
    }

    @Test
    void generatesArgon2HashWithExplicitParametersForLdap() {
        val hash = LdapArgon2Hash.hashForLdap(GIVEN_PASSWORD, 1, 1024, 1, 16, 32);

        assertThat(hash).startsWith("{ARGON2}$argon2id$v=19$m=1024,t=1,p=1$");
        assertThat(LdapArgon2Hash.isValid(hash)).isTrue();
        LdapArgon2Hash.verifyHash(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void detectsValidArgon2Hash() {
        val hash = HashGenerator.using(LDAP_ARGON2).hash(GIVEN_PASSWORD);

        assertThat(LdapArgon2Hash.isValid(hash)).isTrue();
    }

    @Test
    void detectsInvalidArgon2Hashes() {
        assertThat(LdapArgon2Hash.isValid("not-an-argon2-hash")).isFalse();
        assertThat(LdapArgon2Hash.isValid("{ARGON2}not-an-argon2-hash")).isFalse();
        assertThat(LdapArgon2Hash.isValid("{ARGON2}$argon2id$v=19$m=65536,t=3,p=1$short$hash")).isFalse();
    }

    @Test
    void verifiesPasswordAgainstGeneratedSshaHash() {
        val hash = HashGenerator.using(LDAP_SSHA).withRandomSalt().hash(GIVEN_PASSWORD);
        LdapSshaHash.verifyHash(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void avoidsDoubleHashingSshaHashPassword() {
        val hashedPassword = "{SSHA}SNBnIh5QomfgrvDLDwBR+JOcc8Y17H+4";
        val hash = HashGenerator.using(LDAP_SSHA).withRandomSalt().hash(hashedPassword);
        assertThat(hash).isEqualTo(hashedPassword);
    }

    @Test
    void hashesPasswordWhichLooksLikeSshaHashButIsNot() {
        val password = "{SSHA}das-ist-kein-base64-hash";
        val hash = HashGenerator.using(LDAP_SSHA).withRandomSalt().hash(password);
        LdapSshaHash.verifyHash(hash, password); // throws exception if wrong
    }

    @Test
    void verifiesPasswordAgainstRawSshaHashFromOpenLdap() {
        val sha512HashFromOpenLdap = "{SSHA}SNBnIh5QomfgrvDLDwBR+JOcc8Y17H+4";
        LdapSshaHash.verifyHash(sha512HashFromOpenLdap, "QpoGyCeuC1m5X6ew"); // throws exception if wrong
    }

    @Test
    void rejectsInvalidPasswordAgainstGeneratedSshaHash() {
        val hash = HashGenerator.using(LDAP_SSHA).withRandomSalt().hash(GIVEN_PASSWORD);
        val throwable = catchThrowable(() ->
                LdapSshaHash.verifyHash(hash, GIVEN_PASSWORD+"x") // throws exception if wrong
        );
        assertThat(throwable).hasMessage("invalid password");
    }

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
        val hash = HashGenerator.using(LINUX_SHA512).withRandomSalt().hash(GIVEN_PASSWORD);
        LinuxEtcShadowHashGenerator.verify(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void hashesLinuxPasswordWithPredefinedNextSalt() {
        HashGenerator.nextSalt("ooei1HK6JXVaI7KC");

        val hash = HashGenerator.using(LINUX_SHA512).withRandomSalt().hash(GIVEN_PASSWORD);

        assertThat(hash).isEqualTo(GIVEN_LINUX_GENERATED_SHA512_HASH);
    }

    @Test
    void verifiesLinuxHashedPasswordWithGivenSalt() {
        val hash = HashGenerator.using(LINUX_SHA512).withSalt(GIVEN_SALT).hash(GIVEN_PASSWORD);
        LinuxEtcShadowHashGenerator.verify(hash, GIVEN_PASSWORD); // throws exception if wrong
    }

    @Test
    void throwsExceptionForInvalidLinuxPassword() {
        val hash = HashGenerator.using(LINUX_SHA512).withRandomSalt().hash(GIVEN_PASSWORD);
        val throwable = catchThrowable(() ->
                LinuxEtcShadowHashGenerator.verify(hash, WRONG_PASSWORD)
        );
        assertThat(throwable).hasMessage("invalid password");
    }

    @Test
    void generatesLinuxSha512PasswordHash() {
        val hash = HashGenerator.using(LINUX_SHA512).withSalt("ooei1HK6JXVaI7KC").hash(GIVEN_PASSWORD);
        assertThat(hash).isEqualTo(GIVEN_LINUX_GENERATED_SHA512_HASH);
    }

    @Test
    void generatesLinuxYescriptPasswordHash() {
        val hash = HashGenerator.using(LINUX_YESCRYPT).withSalt("wgYACPmBXvlMg2MzeZA0p1").hash(GIVEN_PASSWORD);
        assertThat(hash).isEqualTo(GIVEN_LINUX_GENERATED_YESCRYPT_HASH);
    }

    @Test
    void generatesMySqlNativePasswordHash() {
        val hash = HashGenerator.using(MYSQL_NATIVE).hash("t8L7FULt"); // results in line+branch-coverage
        assertThat(hash).isEqualTo("*F1E107E5C47E0939C7BC941DDE59EDBBDA1F7E39");
    }

    @Test
    void generatePostgreSqlScramPasswordHash() {
        // given the same salt, extracted from the hash as generated by PostgreSQL
        val postgresBase64Salt = Base64.getDecoder().decode(GIVEN_POSTGRESQL_GENERATED_SCRAM_SHA256_HASH.split("\\$")[1].split(":")[1]);

        // when the hash is re-generated via Java
        val hash = HashGenerator.using(SCRAM_SHA256).withSalt(new String(postgresBase64Salt, Charset.forName("latin1"))).hash(GIVEN_PASSWORD);

        // then we are getting the same hash
        assertThat(hash).isEqualTo(GIVEN_POSTGRESQL_GENERATED_SCRAM_SHA256_HASH);
    }
}
