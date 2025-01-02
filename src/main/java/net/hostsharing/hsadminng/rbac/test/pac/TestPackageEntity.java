package net.hostsharing.hsadminng.rbac.test.pac;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;
import net.hostsharing.hsadminng.rbac.test.cust.TestCustomerEntity;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "rbactest", name = "package_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestPackageEntity implements BaseEntity<TestPackageEntity> {

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


    public static RbacSpec rbac() {
        return rbacViewFor("package", TestPackageEntity.class)
                .withIdentityView(SQL.projection("name"))
                .withUpdatableColumns("version", "customerUuid", "description")

                .importEntityAlias("customer", TestCustomerEntity.class, usingDefaultCase(),
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
        rbac().generateWithBaseFileName("2-rbactest/202-rbactest-package/2023-rbactest-package-rbac");
    }
}
