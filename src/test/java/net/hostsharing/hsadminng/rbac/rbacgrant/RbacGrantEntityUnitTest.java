package net.hostsharing.hsadminng.rbac.rbacgrant;

import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RbacGrantEntityUnitTest {

    @Test
    void getRbacGrantId() {
        // given
        final var grantedRoleUuid = UUID.randomUUID();
        final var granteeUserUuid = UUID.randomUUID();
        final var entity = new RbacGrantEntity();
        entity.setGrantedRoleUuid(grantedRoleUuid);
        entity.setGranteeUserUuid(granteeUserUuid);

        // when
        final var grantId = entity.getRbacGrantId();

        // then
        assertThat(grantId).isEqualTo(new RbacGrantId(granteeUserUuid, grantedRoleUuid));
    }

    @Test
    void toDisplayAssumed() {
        // given
        final var entity = new RbacGrantEntity( // @formatter:off
                "GrantER", UUID.randomUUID(),
                "GrantED",  UUID.randomUUID(),
                "GrantEE",  UUID.randomUUID(),
                true,
                "ObjectTable", "ObjectId", UUID.randomUUID(),
                RbacRoleType.admin); // @formatter:on

        // when
        final var display = entity.toDisplay();

        // then
        assertThat(display).isEqualTo("{ grant assumed role GrantED to user GrantEE by role GrantER }");
    }

    @Test
    void toDisplayNotAssumed() {
        // given
        final var entity = new RbacGrantEntity( // @formatter:off
                "GrantER", UUID.randomUUID(),
                "GrantED",  UUID.randomUUID(),
                "GrantEE",  UUID.randomUUID(),
                false,
                "ObjectTable", "ObjectId", UUID.randomUUID(),
                RbacRoleType.owner); // @formatter:on

        // when
        final var display = entity.toDisplay();

        // then
        assertThat(display).isEqualTo("{ grant role GrantED to user GrantEE by role GrantER }");
    }
}
