package net.hostsharing.hsadminng.rbac.test.pac;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.rbac.test.cust.TestCustomerEntity;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;

@Entity
@Table(name = "test_package_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestPackageEntity implements RbacObject {

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customeruuid")
    private TestCustomerEntity customer;

    private String name;

    private String description;


    public static RbacView rbac() {
        return rbacViewFor("package", TestPackageEntity.class)
                .withIdentityView(SQL.projection("name"))
                .withUpdatableColumns("version", "customerUuid", "description")

                .importEntityAlias("customer", TestCustomerEntity.class,
                        dependsOnColumn("customerUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .toRole("customer", ADMIN).grantPermission(INSERT)

                .createRole(OWNER, (with) -> {
                    with.incomingSuperRole("customer", ADMIN);
                    with.permission(DELETE);
                    with.permission(UPDATE);
                })
                .createSubRole(ADMIN)
                .createSubRole(TENANT, (with) -> {
                    with.outgoingSubRole("customer", TENANT);
                    with.permission(SELECT);
                });
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("2-test/202-test-package/2023-test-package-rbac");
    }
}
