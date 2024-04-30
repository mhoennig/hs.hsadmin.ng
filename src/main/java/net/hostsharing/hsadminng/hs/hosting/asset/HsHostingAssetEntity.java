package net.hostsharing.hsadminng.hs.hosting.asset;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.mapper.PatchableMapWrapper;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.CaseDef.inCaseOf;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.CaseDef.inOtherCases;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingCase;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NULLABLE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.TENANT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Builder
@Entity
@Table(name = "hs_hosting_asset_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HsHostingAssetEntity implements Stringifyable, RbacObject {

    private static Stringify<HsHostingAssetEntity> stringify = stringify(HsHostingAssetEntity.class)
            .withProp(HsHostingAssetEntity::getBookingItem)
            .withProp(HsHostingAssetEntity::getType)
            .withProp(HsHostingAssetEntity::getParentAsset)
            .withProp(HsHostingAssetEntity::getIdentifier)
            .withProp(HsHostingAssetEntity::getCaption)
            .withProp(HsHostingAssetEntity::getConfig)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bookingitemuuid")
    private HsBookingItemEntity bookingItem;

    @ManyToOne(optional = true)
    @JoinColumn(name = "parentassetuuid")
    private HsHostingAssetEntity parentAsset;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private HsHostingAssetType type;

    @Column(name = "identifier")
    private String identifier; // vm1234, xyz00, example.org, xyz00_abc

    @Column(name = "caption")
    private String caption;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(columnDefinition = "config")
    private Map<String, Object> config = new HashMap<>();

    @Transient
    private PatchableMapWrapper<Object> configWrapper;

    public PatchableMapWrapper<Object> getConfig() {
        return PatchableMapWrapper.of(configWrapper, (newWrapper) -> {configWrapper = newWrapper; }, config );
    }

    public void putConfig(Map<String, Object> newConfg) {
        PatchableMapWrapper.of(configWrapper, (newWrapper) -> {configWrapper = newWrapper; }, config).assign(newConfg);
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return ofNullable(bookingItem).map(HsBookingItemEntity::toShortString).orElse("D-???????:?") +
                ":" + identifier;
    }

    public static RbacView rbac() {
        return rbacViewFor("asset", HsHostingAssetEntity.class)
                .withIdentityView(SQL.query("""
                        SELECT asset.uuid as uuid, bookingItemIV.idName || '-' || cleanIdentifier(asset.identifier) as idName
                            FROM hs_hosting_asset asset
                            JOIN hs_booking_item_iv bookingItemIV ON bookingItemIV.uuid = asset.bookingItemUuid
                        """))
                .withRestrictedViewOrderBy(SQL.expression("identifier"))
                .withUpdatableColumns("version", "caption", "config")

                .importEntityAlias("bookingItem", HsBookingItemEntity.class, usingDefaultCase(),
                    dependsOnColumn("bookingItemUuid"),
                    directlyFetchedByDependsOnColumn(),
                    NOT_NULL)

                .switchOnColumn("type",
                    inCaseOf(CLOUD_SERVER.name(),
                        then -> then.toRole("bookingItem", AGENT).grantPermission(INSERT)),
                    inCaseOf(MANAGED_SERVER.name(),
                        then -> then.toRole("bookingItem", AGENT).grantPermission(INSERT)),
                    inCaseOf(MANAGED_WEBSPACE.name(), then ->
                        then.importEntityAlias("parentServer", HsHostingAssetEntity.class, usingCase(MANAGED_SERVER),
                                dependsOnColumn("parentAssetUuid"),
                                directlyFetchedByDependsOnColumn(),
                                NULLABLE)
                            .toRole("parentServer", ADMIN).grantPermission(INSERT)
                            .toRole("bookingItem", AGENT).grantPermission(INSERT)
                    ),
                    inOtherCases(then -> {})
                )

                .createRole(OWNER, (with) -> {
                    with.incomingSuperRole("bookingItem", ADMIN);
                    with.permission(DELETE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.permission(UPDATE);
                })
                .createSubRole(TENANT, (with) -> {
                    with.outgoingSubRole("bookingItem", TENANT);
                    with.permission(SELECT);
                })

                .limitDiagramTo("asset", "bookingItem", "bookingItem.debitorRel", "parentServer", "global");
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("7-hs-hosting/701-hosting-asset/7013-hs-hosting-asset-rbac");
    }
}
