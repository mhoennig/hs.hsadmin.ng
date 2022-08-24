package net.hostsharing.hsadminng.rbac.rbacuser;


import static java.util.UUID.randomUUID;

public class TestRbacUser {

    static final RbacUserEntity userxxx = rbacRole("customer-admin@xxx.example.com");
    static final RbacUserEntity userBbb = rbacRole("customer-admin@bbb.example.com");

    static public RbacUserEntity rbacRole(final String userName) {
        return new RbacUserEntity(randomUUID(), userName);
    }
}
