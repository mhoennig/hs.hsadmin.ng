package net.hostsharing.hsadminng.rbac.test.cust;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;

@Entity
@Table(name = "test_customer_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestCustomerEntity implements RbacObject {

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    private String prefix;
    private int reference;

    @Column(name = "adminusername")
    private String adminUserName;

    public static RbacView rbac() {
        return rbacViewFor("customer", TestCustomerEntity.class)
                .withIdentityView(SQL.projection("prefix"))
                .withRestrictedViewOrderBy(SQL.expression("reference"))
                .withUpdatableColumns("reference", "prefix", "adminUserName")
                .toRole("global", ADMIN).grantPermission(INSERT)

                .createRole(OWNER, (with) -> {
                    with.owningUser(CREATOR).unassumed();
                    with.incomingSuperRole(GLOBAL, ADMIN).unassumed();
                    with.permission(DELETE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.permission(UPDATE);
                })
                .createSubRole(TENANT, (with) -> {
                    with.permission(SELECT);
                });
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("2-test/201-test-customer/2013-test-customer-rbac");
    }
}
