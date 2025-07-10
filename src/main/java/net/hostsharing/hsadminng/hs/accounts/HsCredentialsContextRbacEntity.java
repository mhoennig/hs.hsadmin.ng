package net.hostsharing.hsadminng.hs.accounts;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;

import java.io.IOException;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.GUEST;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.REFERRER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.WITHOUT_IMPLICIT_GRANTS;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "hs_accounts", name = "context") // TODO_impl: RBAC rules for _rv do not yet work properly
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "uuid", column = @Column(name = "uuid"))
})
public class HsCredentialsContextRbacEntity extends HsCredentialsContext {

    // TODO_impl: RBAC rules for _rv do not yet work properly (remove the X)
    public static RbacSpec rbacX() {
        return rbacViewFor("credentialsContext", HsCredentialsContextRbacEntity.class)
                .withIdentityView(SQL.projection("type || ':' || qualifier"))
                .withRestrictedViewOrderBy(SQL.expression("type || ':' || qualifier"))
                .withoutUpdatableColumns()
                .createRole(OWNER, WITHOUT_IMPLICIT_GRANTS)
                .createSubRole(ADMIN, WITHOUT_IMPLICIT_GRANTS)
                .createSubRole(REFERRER, WITHOUT_IMPLICIT_GRANTS)
                .toRole(GLOBAL, ADMIN).grantPermission(INSERT)
                .toRole(GLOBAL, ADMIN).grantPermission(DELETE)
                .toRole(GLOBAL, GUEST).grantPermission(SELECT);
    }

    // TODO_impl: RBAC rules for _rv do not yet work properly (remove the X)
    public static void mainX(String[] args) throws IOException {
        rbacX().generateWithBaseFileName("9-hs-global/950-accounts/9513-hs-credentials-rbac");
    }
}
