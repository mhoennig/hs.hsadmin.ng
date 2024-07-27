package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.lowerInclusiveFromPostgresDateRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.upperInclusiveFromPostgresDateRange;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingDefaultCase;
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

@Entity
@Builder(toBuilder = true)
@Table(name = "hs_booking_item_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HsBookingItemEntity implements Stringifyable, RbacObject<HsBookingItemEntity>, PropertiesProvider {

    private static Stringify<HsBookingItemEntity> stringify = stringify(HsBookingItemEntity.class)
            .withProp(HsBookingItemEntity::getProject)
            .withProp(HsBookingItemEntity::getType)
            .withProp(e -> e.getValidity().asString())
            .withProp(HsBookingItemEntity::getCaption)
            .withProp(HsBookingItemEntity::getResources)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projectuuid")
    private HsBookingProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentitemuuid")
    private HsBookingItemEntity parentItem;

    @NotNull
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private HsBookingItemType type;

    @Builder.Default
    @Type(PostgreSQLRangeType.class)
    @Column(name = "validity", columnDefinition = "daterange")
    private Range<LocalDate> validity = Range.closedInfinite(LocalDate.now());

    @Column(name = "caption")
    private String caption;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(columnDefinition = "resources")
    private Map<String, Object> resources = new HashMap<>();

    @OneToMany(cascade = CascadeType.REFRESH, orphanRemoval = true)
    @JoinColumn(name="parentitemuuid", referencedColumnName="uuid")
    private List<HsBookingItemEntity> subBookingItems;

    @OneToOne(mappedBy="bookingItem")
    private HsHostingAssetEntity relatedHostingAsset;

    @Transient
    private PatchableMapWrapper<Object> resourcesWrapper;

    public PatchableMapWrapper<Object> getResources() {
        return PatchableMapWrapper.of(resourcesWrapper, (newWrapper) -> {resourcesWrapper = newWrapper; }, resources );
    }

    public void putResources(Map<String, Object> newResources) {
        getResources().assign(newResources);
    }

    public void setValidFrom(final LocalDate validFrom) {
        setValidity(toPostgresDateRange(validFrom, getValidTo()));
    }

    public void setValidTo(final LocalDate validTo) {
        setValidity(toPostgresDateRange(getValidFrom(), validTo));
    }

    public LocalDate getValidFrom() {
        return lowerInclusiveFromPostgresDateRange(getValidity());
    }

    public LocalDate getValidTo() {
        return upperInclusiveFromPostgresDateRange(getValidity());
    }

    @Override
    public Map<String, Object> directProps() {
        return resources;
    }

    @Override
    public Object getContextValue(final String propName) {
        final var v = resources.get(propName);
        if (v!= null) {
            return v;
        }
        if (parentItem!=null) {
            return parentItem.getResources().get(propName);
        }
        return emptyMap();
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return ofNullable(relatedProject()).map(HsBookingProjectEntity::toShortString).orElse("D-???????-?") +
                ":" + caption;
    }

    private HsBookingProjectEntity relatedProject() {
        if (project != null) {
            return project;
        }
        return parentItem == null ? null : parentItem.relatedProject();
    }

    public HsBookingProjectEntity getRelatedProject() {
        return project != null ? project
                : parentItem != null ? parentItem.getRelatedProject()
                : null; // can be the case for technical assets like IP-numbers
    }

    public static RbacView rbac() {
        return rbacViewFor("bookingItem", HsBookingItemEntity.class)
                .withIdentityView(SQL.projection("caption"))
                .withRestrictedViewOrderBy(SQL.expression("validity"))
                .withUpdatableColumns("version", "caption", "validity", "resources")
                .toRole("global", ADMIN).grantPermission(INSERT) // TODO.impl: Why is this necessary to insert test data?
                .toRole("global", ADMIN).grantPermission(DELETE)

                .importEntityAlias("project", HsBookingProjectEntity.class, usingDefaultCase(),
                        dependsOnColumn("projectUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)
                .toRole("project", ADMIN).grantPermission(INSERT)

                .importEntityAlias("parentItem", HsBookingItemEntity.class, usingDefaultCase(),
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

                .limitDiagramTo("bookingItem", "project", "global");
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("6-hs-booking/630-booking-item/6303-hs-booking-item-rbac");
    }
}
