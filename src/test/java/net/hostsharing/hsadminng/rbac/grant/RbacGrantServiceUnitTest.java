package net.hostsharing.hsadminng.rbac.grant;

import net.hostsharing.hsadminng.rbac.role.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.role.RbacRoleType;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RbacGrantServiceUnitTest {

    private static final UUID GIVEN_ROLE_UUID = UUID.randomUUID();
    private static final UUID GIVEN_SUBJECT_UUID = UUID.randomUUID();

    @Mock
    private RbacGrantRepository rbacGrantRepo;

    @InjectMocks
    private RbacGrantService rbacGrantService;

    @Test
    void grantToSubjectPersistsAnAssumedGrantForRoleAndSubject() {

        // given
        final var role = new RbacRoleEntity(
                GIVEN_ROLE_UUID,
                UUID.randomUUID(),
                "rbactest.customer",
                "xxx",
                RbacRoleType.ADMIN,
                "rbactest.customer#xxx:ADMIN");
        final var subject = RbacSubjectEntity.builder().uuid(GIVEN_SUBJECT_UUID).build();

        // when
        rbacGrantService.grant(role).to(subject);

        // then
        final var grantCaptor = ArgumentCaptor.forClass(RbacGrantEntity.class);
        verify(rbacGrantRepo).save(grantCaptor.capture());

        assertThat(grantCaptor.getValue())
                .extracting(
                        RbacGrantEntity::getGrantedRoleUuid,
                        RbacGrantEntity::getGranteeSubjectUuid,
                        RbacGrantEntity::isAssumed)
                .containsExactly(GIVEN_ROLE_UUID, GIVEN_SUBJECT_UUID, true);
    }
}
