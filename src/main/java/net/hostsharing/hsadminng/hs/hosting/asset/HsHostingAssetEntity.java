package net.hostsharing.hsadminng.hs.hosting.asset;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.validation.PropertiesProvider;
import net.hostsharing.hsadminng.mapper.PatchableMapWrapper;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.Type;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.CaseDef.inCaseOf;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NULLABLE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.GUEST;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.REFERRER;
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
public class HsHostingAssetEntity implements Stringifyable, RbacObject, PropertiesProvider {

    private static Stringify<HsHostingAssetEntity> stringify = stringify(HsHostingAssetEntity.class)
            .withProp(HsHostingAssetEntity::getType)
            .withProp(HsHostingAssetEntity::getIdentifier)
            .withProp(HsHostingAssetEntity::getCaption)
            .withProp(HsHostingAssetEntity::getParentAsset)
            .withProp(HsHostingAssetEntity::getAssignedToAsset)
            .withProp(HsHostingAssetEntity::getBookingItem)
            .withProp(HsHostingAssetEntity::getConfig)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingitemuuid")
    private HsBookingItemEntity bookingItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentassetuuid")
    private HsHostingAssetEntity parentAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignedtoassetuuid")
    private HsHostingAssetEntity assignedToAsset;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private HsHostingAssetType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarmcontactuuid")
    private HsOfficeContactEntity alarmContact;

    @OneToMany(cascade = CascadeType.REFRESH, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "parentassetuuid", referencedColumnName = "uuid")
    private List<HsHostingAssetEntity> subHostingAssets;

    @Column(name = "identifier")
    private String identifier; // e.g. vm1234, xyz00, example.org, xyz00_abc

    @Column(name = "caption")
    private String caption;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(columnDefinition = "config")
    private Map<String, Object> config = new HashMap<>();

    @Transient
    private PatchableMapWrapper<Object> configWrapper;

    @Transient
    private boolean isLoaded;

    @PostLoad
    public void markAsLoaded() {
        this.isLoaded = true;
    }

    public HsBookingProjectEntity getRelatedProject() {
        return Optional.ofNullable(bookingItem)
                .map(HsBookingItemEntity::getRelatedProject)
                .orElseGet(() -> Optional.ofNullable(parentAsset)
                        .map(HsHostingAssetEntity::getRelatedProject)
                        .orElse(null));
    }

    public PatchableMapWrapper<Object> getConfig() {
        return PatchableMapWrapper.of(configWrapper, (newWrapper) -> {configWrapper = newWrapper;}, config);
    }

    public void putConfig(Map<String, Object> newConfig) {
        PatchableMapWrapper.of(configWrapper, (newWrapper) -> {configWrapper = newWrapper;}, config).assign(newConfig);
    }

    @Override
    public Map<String, Object> directProps() {
        return config;
    }

    @Override
    public Object getContextValue(final String propName) {
        final var v = config.get(propName);
        if (v != null) {
            return v;
        }

        if (bookingItem != null) {
            return bookingItem.getResources().get(propName);
        }
        if (parentAsset != null && parentAsset.getBookingItem() != null) {
            return parentAsset.getBookingItem().getResources().get(propName);
        }
        return emptyMap();
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return type + ":" + identifier;
    }

    public static RbacView rbac() {
        return rbacViewFor("asset", HsHostingAssetEntity.class)
                .withIdentityView(SQL.projection("identifier"))
                .withRestrictedViewOrderBy(SQL.expression("identifier"))
                .withUpdatableColumns("version", "caption", "config", "assignedToAssetUuid", "alarmContactUuid")
                .toRole(GLOBAL, ADMIN).grantPermission(INSERT) // TODO.impl: Why is this necessary to insert test data?

                .importEntityAlias("bookingItem", HsBookingItemEntity.class, usingDefaultCase(),
                        dependsOnColumn("bookingItemUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)

                .importEntityAlias("parentAsset", HsHostingAssetEntity.class, usingDefaultCase(),
                        dependsOnColumn("parentAssetUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)
                .toRole("parentAsset", ADMIN).grantPermission(INSERT)

                .importEntityAlias("assignedToAsset", HsHostingAssetEntity.class, usingDefaultCase(),
                        dependsOnColumn("assignedToAssetUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)

                .importEntityAlias("alarmContact", HsOfficeContactEntity.class, usingDefaultCase(),
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
                        "global");
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("7-hs-hosting/701-hosting-asset/7013-hs-hosting-asset-rbac");
    }
}
