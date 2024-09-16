package net.hostsharing.hsadminng.rbac.grant;

import net.hostsharing.hsadminng.rbac.role.RbacRoleType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RbacGrantEntityUnitTest {

    @Test
    void getRbacGrantId() {
        // given
        final var grantedRoleUuid = UUID.randomUUID();
        final var granteeSubjectUuid = UUID.randomUUID();
        final var entity = new RbacGrantEntity();
        entity.setGrantedRoleUuid(grantedRoleUuid);
        entity.setGranteeSubjectUuid(granteeSubjectUuid);

        // when
        final var grantId = entity.getRbacGrantId();

        // then
        assertThat(grantId).isEqualTo(new RbacGrantId(granteeSubjectUuid, grantedRoleUuid));
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
                RbacRoleType.ADMIN); // @formatter:on

        // when
        final var display = entity.toDisplay();

        // then
        assertThat(display).isEqualTo("{ grant role:GrantED to user:GrantEE by role:GrantER and assume }");
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
                RbacRoleType.OWNER); // @formatter:on

        // when
        final var display = entity.toDisplay();

        // then
        assertThat(display).isEqualTo("{ grant role:GrantED to user:GrantEE by role:GrantER }");
    }
}
