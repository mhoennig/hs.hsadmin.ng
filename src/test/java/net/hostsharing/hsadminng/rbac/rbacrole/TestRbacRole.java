package net.hostsharing.hsadminng.rbac.rbacrole;

import static java.util.UUID.randomUUID;

public class TestRbacRole {

    public static final RbacRoleEntity hostmasterRole = rbacRole("global", "global", RbacRoleType.ADMIN);
    static final RbacRoleEntity customerXxxOwner = rbacRole("test_customer", "xxx", RbacRoleType.OWNER);
    static final RbacRoleEntity customerXxxAdmin = rbacRole("test_customer", "xxx", RbacRoleType.ADMIN);

    static public RbacRoleEntity rbacRole(final String objectTable, final String objectIdName, final RbacRoleType roleType) {
        return new RbacRoleEntity(randomUUID(), randomUUID(), objectTable, objectIdName, roleType, objectTable+'#'+objectIdName+':'+roleType);
    }
}
