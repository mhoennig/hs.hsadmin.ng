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
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.mapper.PatchableMapWrapper;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.lowerInclusiveFromPostgresDateRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.upperInclusiveFromPostgresDateRange;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.TENANT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Builder
@Entity
@Table(name = "hs_booking_item_rv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HsBookingItemEntity implements Stringifyable, RbacObject {

    private static Stringify<HsBookingItemEntity> stringify = stringify(HsBookingItemEntity.class)
            .withProp(e -> e.getDebitor().toShortString())
            .withProp(e -> e.getValidity().asString())
            .withProp(HsBookingItemEntity::getCaption)
            .withProp(HsBookingItemEntity::getResources)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "debitoruuid")
    private HsOfficeDebitorEntity debitor;

    @Builder.Default
    @Type(PostgreSQLRangeType.class)
    @Column(name = "validity", columnDefinition = "daterange")
    private Range<LocalDate> validity = Range.emptyRange(LocalDate.class);

    @Column(name = "caption")
    private String caption;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @Type(JsonType.class)
    @Column(columnDefinition = "resources")
    private Map<String, Object> resources = new HashMap<>();

    @Transient
    private PatchableMapWrapper resourcesWrapper;

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

    public PatchableMapWrapper getResources() {
        if ( resourcesWrapper == null ) {
            resourcesWrapper = new PatchableMapWrapper(resources);
        }
        return resourcesWrapper;
    }

    public void putResources(Map<String, Object> entries) {
        if ( resourcesWrapper == null ) {
            resourcesWrapper = new PatchableMapWrapper(resources);
        }
        resourcesWrapper.assign(entries);
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return ofNullable(debitor).map(HsOfficeDebitorEntity::toShortString).orElse("D-???????") +
                ":" + caption;
    }

    public static RbacView rbac() {
        return rbacViewFor("bookingItem", HsBookingItemEntity.class)
                .withIdentityView(SQL.query("""
                        SELECT bookingItem.uuid as uuid, debitorIV.idName || '-' || cleanIdentifier(bookingItem.caption) as idName
                            FROM hs_booking_item bookingItem
                            JOIN hs_office_debitor_iv debitorIV ON debitorIV.uuid = bookingItem.debitorUuid
                        """))
                .withRestrictedViewOrderBy(SQL.expression("validity"))
                .withUpdatableColumns("version", "caption", "validity", "resources")

                .importEntityAlias("debitor", HsOfficeDebitorEntity.class,
                        dependsOnColumn("debitorUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)

                .importEntityAlias("debitorRel", HsOfficeRelationEntity.class,
                        dependsOnColumn("debitorUuid"),
                        fetchedBySql("""
                                SELECT ${columns}
                                    FROM hs_office_relation debitorRel
                                    JOIN hs_office_debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
                                    WHERE debitor.uuid = ${REF}.debitorUuid
                                """),
                        NOT_NULL)
                .toRole("debitorRel", ADMIN).grantPermission(INSERT)
                .toRole("global", ADMIN).grantPermission(DELETE)

                .createRole(OWNER, (with) -> {
                    with.incomingSuperRole("debitorRel", AGENT);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.incomingSuperRole("debitorRel", AGENT);
                    with.permission(UPDATE);
                })
                .createSubRole(AGENT)
                .createSubRole(TENANT, (with) -> {
                    with.outgoingSubRole("debitorRel", TENANT);
                    with.permission(SELECT);
                });
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("6-hs-booking/601-booking-item/6013-hs-booking-item-rbac");
    }
}
