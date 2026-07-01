package net.hostsharing.hsadminng.rbac.subject;


import static java.util.UUID.randomUUID;

public class TestRbacSubject {

    static final RbacSubjectEntity userxxx = rbacRole("tst-customer_admin_xxx");
    static final RbacSubjectEntity userBbb = rbacRole("tst-customer_admin_bbb");

    static public RbacSubjectEntity rbacRole(final String userName) {
        return RbacSubjectEntity.builder().uuid(randomUUID()).name(userName).build();
    }
}
