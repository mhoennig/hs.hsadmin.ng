package net.hostsharing.hsadminng.hs.booking.item;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRbacEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NULLABLE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.TENANT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "hs_booking", name = "item_rv")
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "uuid", column = @Column(name = "uuid"))
})
public class HsBookingItemRbacEntity extends HsBookingItem {

    public static RbacSpec rbac() {
        return rbacViewFor("bookingItem", HsBookingItemRbacEntity.class)
                .withIdentityView(SQL.projection("caption"))
                .withRestrictedViewOrderBy(SQL.expression("validity"))
                .withUpdatableColumns("version", "caption", "validity", "resources")
                .toRole(GLOBAL, ADMIN).grantPermission(INSERT) // TODO.impl: Why is this necessary to insert test data?
                .toRole(GLOBAL, ADMIN).grantPermission(DELETE)

                .importEntityAlias("project", HsBookingProjectRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("projectUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)
                .toRole("project", ADMIN).grantPermission(INSERT)

                .importEntityAlias("parentItem", HsBookingItemRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("parentItemUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)
                .toRole("parentItem", ADMIN).grantPermission(INSERT)

                .createRole(OWNER, (with) -> {
                    with.incomingSuperRole("project", AGENT);
                    with.incomingSuperRole("parentItem", AGENT);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.permission(UPDATE);
                })
                .createSubRole(AGENT)
                .createSubRole(TENANT, (with) -> {
                    with.outgoingSubRole("project", TENANT);
                    with.outgoingSubRole("parentItem", TENANT);
                    with.permission(SELECT);
                })

                .limitDiagramTo("bookingItem", "project", "rbac.global");
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("6-hs-booking/630-booking-item/6303-hs-booking-item-rbac");
    }
}
