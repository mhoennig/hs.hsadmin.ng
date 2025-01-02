package net.hostsharing.hsadminng.hs.hosting.asset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRbacEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.CaseDef.inCaseOf;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NULLABLE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.GUEST;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.REFERRER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.TENANT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "hs_hosting", name = "asset_rv")
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
public class HsHostingAssetRbacEntity extends HsHostingAsset {

    public static RbacSpec rbac() {
        return rbacViewFor("asset", HsHostingAssetRbacEntity.class)
                .withIdentityView(SQL.projection("identifier"))
                .withRestrictedViewOrderBy(SQL.expression("identifier"))
                .withUpdatableColumns("version", "caption", "config", "assignedToAssetUuid", "alarmContactUuid")
                .toRole(GLOBAL, ADMIN).grantPermission(INSERT) // TODO.impl: Why is this necessary to insert test data?

                .importEntityAlias("bookingItem", HsBookingItemRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("bookingItemUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)

                .importEntityAlias("parentAsset", HsHostingAssetRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("parentAssetUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)
                .toRole("parentAsset", ADMIN).grantPermission(INSERT)

                .importEntityAlias("assignedToAsset", HsHostingAssetRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("assignedToAssetUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)

                .importEntityAlias("alarmContact", HsOfficeContactRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("alarmContactUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)

                .switchOnColumn(
                        "type",
                        inCaseOf("DOMAIN_SETUP", then -> {
                            then.toRole(GLOBAL, GUEST).grantPermission(INSERT);
                        })
                )

                .createRole(OWNER, (with) -> {
                    with.owningUser(CREATOR);
                    with.incomingSuperRole(GLOBAL, ADMIN).unassumed(); // TODO.spec: replace by a better solution
                    with.incomingSuperRole("bookingItem", ADMIN);
                    with.incomingSuperRole("parentAsset", ADMIN);
                    with.permission(DELETE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.incomingSuperRole("bookingItem", AGENT);
                    with.incomingSuperRole("parentAsset", AGENT);
                    with.permission(UPDATE);
                })
                .createSubRole(AGENT, (with) -> {
                    with.incomingSuperRole("assignedToAsset", AGENT); // TODO.spec: or ADMIN?
                    with.outgoingSubRole("assignedToAsset", TENANT);
                    with.outgoingSubRole("alarmContact", REFERRER);
                })
                .createSubRole(TENANT, (with) -> {
                    with.outgoingSubRole("bookingItem", TENANT);
                    with.outgoingSubRole("parentAsset", TENANT);
                    with.incomingSuperRole("alarmContact", ADMIN);
                    with.permission(SELECT);
                })

                .limitDiagramTo(
                        "asset",
                        "bookingItem",
                        "bookingItem.debitorRel",
                        "parentAsset",
                        "assignedToAsset",
                        "alarmContact",
                        "rbac.global");
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("7-hs-hosting/701-hosting-asset/7013-hs-hosting-asset-rbac");
    }
}
