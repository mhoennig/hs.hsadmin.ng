package net.hostsharing.hsadminng.rbac.role;

import lombok.val;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Table;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WithRoleIdUnitTest {

    @Test
    void roleIdUsesAnnotatedTableAndUuid() {
        // given
        val uuid = UUID.randomUUID();
        val entity = new GivenEntity(uuid);

        // when
        val roleId = entity.roleId(RbacRoleType.ADMIN);

        // then
        assertThat(roleId).isEqualTo("rbactest.given#" + uuid + ":ADMIN");
    }

    @Test
    void roleIdRejectsMissingUuid() {
        // given
        val entity = new GivenEntity(null);

        // then
        assertThatThrownBy(() -> entity.roleId(RbacRoleType.ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("UUID missing => role can't be determined");
    }

    @Table(schema = "rbactest", name = "given")
    private record GivenEntity(UUID uuid) implements WithRoleId {

        @Override
        public UUID getUuid() {
            return uuid;
        }
    }
}
