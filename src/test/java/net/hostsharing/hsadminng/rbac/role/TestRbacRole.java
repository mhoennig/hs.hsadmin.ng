package net.hostsharing.hsadminng.rbac.role;

import lombok.val;

import static java.util.UUID.randomUUID;

public class TestRbacRole {

    public static final RbacRoleEntity hostmasterRole = rbacRole("rbac.global", "global", RbacRoleType.ADMIN);
    static final RbacRoleEntity customerXxxOwner = rbacRole("rbactest.customer", "xxx", RbacRoleType.OWNER);
    static final RbacRoleEntity customerXxxAdmin = rbacRole("rbactest.customer", "xxx", RbacRoleType.ADMIN);

    static public RbacRoleEntity rbacRole(final String objectTable, final String objectIdName, final RbacRoleType roleType) {
        val objectUuid = randomUUID();
        return new RbacRoleEntity(
                randomUUID(),
                objectUuid,
                objectTable,
                objectIdName,
                roleType,
                objectTable + '#' + objectUuid + ':' + roleType,
                objectTable + '#' + objectIdName + ':' + roleType);
    }
}
