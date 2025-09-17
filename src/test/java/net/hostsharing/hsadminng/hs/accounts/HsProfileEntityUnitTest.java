package net.hostsharing.hsadminng.hs.accounts;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class HsProfileEntityUnitTest {

    static final HsProfileEntity GIVEN_PROFILE_ENTITY = HsProfileEntity.builder()
            .uuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .subject(
                    RealSubjectEntity.builder().uuid(UUID.randomUUID()).name("testSubject").build())
            .person(
                    HsOfficePersonRealEntity.builder()
                            .personType(HsOfficePersonType.NATURAL_PERSON)
                            .familyName("Miller")
                            .givenName("John")
                            .build()
            )
            .emailAddress("john.miller@example.com")
            .smsNumber("+49 1234567890")
            .globalUid(10001)
            .globalUid(20002)
            .phonePassword("hello world")
            .totpSecrets(List.of("secret1", "secret2"))
            .active(true)
            .build();

    @Test
    void toShortStringContainsJustTypeAndQualifier() {
        assertThat(GIVEN_PROFILE_ENTITY.toShortString()).isEqualTo("true:john.miller@example.com:20002");
    }

    @Test
    void toStringContainsAllPropertiesExceptUuidAndPasswordHash() {
        assertThat(GIVEN_PROFILE_ENTITY.toString()).isEqualTo("profile(true, john.miller@example.com, [secret1, secret2], hello world, +49 1234567890)");
    }

    @Test
    void setPasswordSetsPasswordHash() {
        val profile = HsProfileEntity.builder().build();
        profile.setPassword("my password");
        assertThat(profile.getPasswordHash()).startsWith("{SSHA}");
    }

    @Test
    void acceptsValidSshaPasswordHash() {
        val givenSshaHash = "{SSHA}SNBnIh5QomfgrvDLDwBR+JOcc8Y17H+4";
        val profile = HsProfileEntity.builder().build();
        profile.setPasswordHash(givenSshaHash);
        assertThat(profile.getPasswordHash()).isEqualTo(givenSshaHash);
    }

    @Test
    void acceptsValidArgon2PasswordHash() {
        val givenArgon2Hash = "{ARGON2}$argon2id$v=19$m=65536,t=3,p=1$pEabRksh7EJQV+OwPR5n7Q$83qQtZe2J8+fteWm7g/uvXksfhJKGsipZFsuAaJtBjs";
        val profile = HsProfileEntity.builder().build();
        profile.setPasswordHash(givenArgon2Hash);
        assertThat(profile.getPasswordHash()).isEqualTo(givenArgon2Hash);
    }

    @Test
    void rejectInvalidPasswordHash() {
        val profile = HsProfileEntity.builder().build();
        val throwable = assertThrows(
                ValidationException.class,
                () -> profile.setPasswordHash("{whatever} but not a valid hash"));
        assertThat(throwable.getMessage()).isEqualTo("passwordHash must be SSHA or ARGON2 hash valid for LDAP");
    }
}
