package net.hostsharing.hsadminng.rbac.rbacrole;

import static java.util.UUID.randomUUID;

public class TestRbacRole {

    public static final RbacRoleEntity hostmasterRole = rbacRole("global", "global", RbacRoleType.admin);
    static final RbacRoleEntity customerXxxOwner = rbacRole("test_customer", "xxx", RbacRoleType.owner);
    static final RbacRoleEntity customerXxxAdmin = rbacRole("test_customer", "xxx", RbacRoleType.admin);

    static public RbacRoleEntity rbacRole(final String objectTable, final String objectIdName, final RbacRoleType roleType) {
        return new RbacRoleEntity(randomUUID(), randomUUID(), objectTable, objectIdName, roleType, objectTable+'#'+objectIdName+'.'+roleType);
    }
}
