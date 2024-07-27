package net.hostsharing.hsadminng.rbac.test.dom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.rbac.test.pac.TestPackageEntity;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;

@Entity
@Table(name = "test_domain_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestDomainEntity implements RbacObject<TestDomainEntity> {

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "packageuuid")
    private TestPackageEntity pac;

    private String name;

    private String description;

    public static RbacView rbac() {
        return rbacViewFor("domain", TestDomainEntity.class)
                .withIdentityView(SQL.projection("name"))
                .withUpdatableColumns("version", "packageUuid", "description")

                .importEntityAlias("package", TestPackageEntity.class, usingDefaultCase(),
                        dependsOnColumn("packageUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .toRole("package", ADMIN).grantPermission(INSERT)

                .createRole(OWNER, (with) -> {
                    with.incomingSuperRole("package", ADMIN);
                    with.outgoingSubRole("package", TENANT);
                    with.permission(DELETE);
                    with.permission(UPDATE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.outgoingSubRole("package", TENANT);
                    with.permission(SELECT);
                });
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("2-test/203-test-domain/2033-test-domain-rbac");
    }
}
