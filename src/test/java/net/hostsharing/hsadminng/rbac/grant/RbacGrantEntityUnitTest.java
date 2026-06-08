package net.hostsharing.hsadminng.rbac.grant;

import net.hostsharing.hsadminng.rbac.role.RbacRoleType;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RbacGrantEntityUnitTest {

    @Test
    void getRbacGrantId() {
        // given
        val grantedRoleUuid = UUID.randomUUID();
        val granteeSubjectUuid = UUID.randomUUID();
        val entity = new RbacGrantEntity();
        entity.setGrantedRoleUuid(grantedRoleUuid);
        entity.setGranteeSubjectUuid(granteeSubjectUuid);

        // when
        val grantId = entity.getRbacGrantId();

        // then
        assertThat(grantId).isEqualTo(new RbacGrantId(granteeSubjectUuid, grantedRoleUuid));
    }

    @Test
    void toDisplayAssumed() {
        // given
        val entity = new RbacGrantEntity( // @formatter:off
                "GrantER", UUID.randomUUID(),
                "GrantED",  UUID.randomUUID(),
                "GrantEE",  UUID.randomUUID(),
                true,
                "ObjectTable", "ObjectId", UUID.randomUUID(),
                RbacRoleType.ADMIN); // @formatter:on

        // when
        val display = entity.toDisplay();

        // then
        assertThat(display).isEqualTo("{ grant role:GrantED to user:GrantEE by role:GrantER and assume }");
    }

    @Test
    void toDisplayNotAssumed() {
        // given
        val entity = new RbacGrantEntity( // @formatter:off
                "GrantER", UUID.randomUUID(),
                "GrantED",  UUID.randomUUID(),
                "GrantEE",  UUID.randomUUID(),
                false,
                "ObjectTable", "ObjectId", UUID.randomUUID(),
                RbacRoleType.OWNER); // @formatter:on

        // when
        val display = entity.toDisplay();

        // then
        assertThat(display).isEqualTo("{ grant role:GrantED to user:GrantEE by role:GrantER }");
    }

    @Test
    void rawGrantToDisplayWithGrantingRole() {
        // given
        val entity = RawRbacGrantEntity.builder()
                .descendantIdName("role:rbactest.package#pac00:TENANT")
                .ascendantIdName("role:rbactest.customer#xxx:ADMIN")
                .grantedByRoleUuid(UUID.randomUUID())
                .grantedByRoleIdName("role:rbac.global#global:ADMIN")
                .assumed(true)
                .build();

        // when
        val display = entity.toDisplay();

        // then
        assertThat(display).isEqualTo("""
                { grant role:rbactest.package#pac00:TENANT to role:rbactest.customer#xxx:ADMIN by role:rbac.global#global:ADMIN and assume }\
                """);
    }

    @Test
    void rawGrantToDisplayWithSystemGrant() {
        // given
        val entity = RawRbacGrantEntity.builder()
                .descendantIdName("role:rbactest.customer#xxx:OWNER")
                .ascendantIdName("user:customer-admin@example.org")
                .assumed(false)
                .build();

        // when
        val display = entity.toDisplay();

        // then
        assertThat(display).isEqualTo(
                "{ grant role:rbactest.customer#xxx:OWNER to user:customer-admin@example.org by system }");
    }

    @Test
    void rawGrantDisplaysAreSortedAndDistinct() {
        // given
        val first = RawRbacGrantEntity.builder()
                .descendantIdName("role:b")
                .ascendantIdName("user:x")
                .build();
        val second = RawRbacGrantEntity.builder()
                .descendantIdName("role:a")
                .ascendantIdName("user:x")
                .build();
        val duplicate = RawRbacGrantEntity.builder()
                .descendantIdName("role:a")
                .ascendantIdName("user:x")
                .build();

        // when
        val displays = RawRbacGrantEntity.distinctGrantDisplaysOf(List.of(first, second, duplicate));

        // then
        assertThat(displays).containsExactly(
                "{ grant role:a to user:x by system }",
                "{ grant role:b to user:x by system }");
    }

    @Test
    void rawGrantComparesByUuid() {
        // given
        val lowerUuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        val higherUuid = UUID.fromString("00000000-0000-0000-0000-000000000002");
        val lower = RawRbacGrantEntity.builder().uuid(lowerUuid).build();
        val higher = RawRbacGrantEntity.builder().uuid(higherUuid).build();

        // then
        assertThat(lower.compareTo(higher)).isNegative();
        assertThat(higher.compareTo(lower)).isPositive();
    }
}
