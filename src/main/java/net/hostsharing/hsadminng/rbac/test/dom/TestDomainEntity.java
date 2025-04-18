package net.hostsharing.hsadminng.rbac.test.dom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;
import net.hostsharing.hsadminng.rbac.test.pac.TestPackageEntity;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "rbactest", name = "domain_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestDomainEntity implements BaseEntity<TestDomainEntity> {

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

    public static RbacSpec rbac() {
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
        rbac().generateWithBaseFileName("2-rbactest/203-rbactest-domain/2033-rbactest-domain-rbac");
    }
}
