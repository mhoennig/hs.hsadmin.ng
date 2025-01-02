package net.hostsharing.hsadminng.rbac.test.cust;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "rbactest", name = "customer_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TestCustomerEntity implements BaseEntity<TestCustomerEntity> {

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    private String prefix;
    private int reference;

    @Column(name = "adminusername")
    private String adminUserName;

    public static RbacSpec rbac() {
        return rbacViewFor("customer", TestCustomerEntity.class)
                .withIdentityView(SQL.projection("prefix"))
                .withRestrictedViewOrderBy(SQL.expression("reference"))
                .withUpdatableColumns("reference", "prefix", "adminUserName")
                .toRole("rbac.global", ADMIN).grantPermission(INSERT)

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
        rbac().generateWithBaseFileName("2-rbactest/201-rbactest-customer/2013-rbactest-customer-rbac");
    }
}
