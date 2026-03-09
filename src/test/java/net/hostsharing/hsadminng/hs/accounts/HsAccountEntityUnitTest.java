package net.hostsharing.hsadminng.hs.accounts;

import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HsAccountEntityUnitTest {

    static final HsAccountEntity GIVEN_ACCOUNT_ENTITY = HsAccountEntity.builder()
            .uuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .subject(
                    RealSubjectEntity.builder().uuid(UUID.randomUUID()).name("test-subject").build())
            .person(
                    HsOfficePersonRealEntity.builder()
                            .personType(HsOfficePersonType.NATURAL_PERSON)
                            .familyName("Miller")
                            .givenName("John")
                            .build()
            )
            .globalUid(10001)
            .globalUid(20002)
            .build();

    @Test
    void toShortStringContainsJustTheSubjectName() {
        assertThat(GIVEN_ACCOUNT_ENTITY.toShortString()).isEqualTo("test-subject");
    }

    @Test
    void toStringContainsJustTheSubjectNam() {
        assertThat(GIVEN_ACCOUNT_ENTITY.toString()).isEqualTo("account(test-subject)");
    }
}
