package net.hostsharing.hsadminng.rbac.rbacuser;


import static java.util.UUID.randomUUID;

public class TestRbacUser {

    static final RbacUserEntity userAaa = rbacRole("admin@aaa.example.com");
    static final RbacUserEntity userBbb = rbacRole("admin@bbb.example.com");

    static public RbacUserEntity rbacRole(final String userName) {
        return new RbacUserEntity(randomUUID(), userName);
    }
}
