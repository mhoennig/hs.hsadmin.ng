package net.hostsharing.hsadminng.rbac.subject;


import static java.util.UUID.randomUUID;

public class TestRbacSubject {

    static final RbacSubjectEntity userxxx = rbacRole("customer-admin@xxx.example.com");
    static final RbacSubjectEntity userBbb = rbacRole("customer-admin@bbb.example.com");

    static public RbacSubjectEntity rbacRole(final String userName) {
        return new RbacSubjectEntity(randomUUID(), userName);
    }
}
