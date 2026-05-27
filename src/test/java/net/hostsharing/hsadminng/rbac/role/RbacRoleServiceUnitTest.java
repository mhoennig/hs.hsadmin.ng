package net.hostsharing.hsadminng.rbac.role;

import net.hostsharing.hsadminng.persistence.BaseEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RbacRoleServiceUnitTest {

    private static final UUID GIVEN_ENTITY_UUID = UUID.randomUUID();

    @Mock
    private RbacRoleRepository rbacRoleRepo;

    @Mock
    private BaseEntity<?> rbacEntity;

    @InjectMocks
    private RbacRoleService rbacRoleService;

    @Test
    void rbacRoleFactoryReturnsRoleFoundByEntityUuidAndRoleType() {

        // given
        final var givenRole = TestRbacRole.rbacRole("rbactest.customer", "xxx", RbacRoleType.ADMIN);
        given(rbacEntity.getUuid()).willReturn(GIVEN_ENTITY_UUID);
        given(rbacRoleRepo.findByObjectUuidAndRoleType(GIVEN_ENTITY_UUID, RbacRoleType.ADMIN)).willReturn(givenRole);

        // when
        final var result = rbacRoleService.rbacRole(rbacEntity, RbacRoleType.ADMIN);

        // then
        assertThat(result).isSameAs(givenRole);
        verify(rbacRoleRepo).findByObjectUuidAndRoleType(GIVEN_ENTITY_UUID, RbacRoleType.ADMIN);
    }

    @Test
    void rbacRoleFactoryThrowsInternalServerErrorIfRoleIsMissing() {

        // given
        given(rbacEntity.getUuid()).willReturn(GIVEN_ENTITY_UUID);
        given(rbacRoleRepo.findByObjectUuidAndRoleType(GIVEN_ENTITY_UUID, RbacRoleType.ADMIN)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> rbacRoleService.rbacRole(rbacEntity, RbacRoleType.ADMIN))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(exception.getReason()).isEqualTo(
                            "no ADMIN role not found for %s %s".formatted(GIVEN_ENTITY_UUID, GIVEN_ENTITY_UUID));
                });
        verify(rbacRoleRepo).findByObjectUuidAndRoleType(GIVEN_ENTITY_UUID, RbacRoleType.ADMIN);
    }
}
