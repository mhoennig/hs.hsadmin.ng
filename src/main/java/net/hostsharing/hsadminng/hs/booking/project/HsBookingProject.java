package net.hostsharing.hsadminng.hs.booking.project;

import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.rbac.rbacobject.BaseEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.DEBITOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingCase;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(builderMethodName = "baseBuilder", toBuilder = true)
public abstract class HsBookingProject implements Stringifyable, BaseEntity<HsBookingProject> {

    private static Stringify<HsBookingProject> stringify = stringify(HsBookingProject.class)
            .withProp(HsBookingProject::getDebitor)
            .withProp(HsBookingProject::getCaption)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "debitoruuid")
    private HsBookingDebitorEntity debitor;

    @Column(name = "caption")
    private String caption;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return ofNullable(debitor).map(HsBookingDebitorEntity::toShortString).orElse("D-???????") +
                ":" + caption;
    }

    public static RbacView rbac() {
        return rbacViewFor("project", HsBookingProject.class)
                .withIdentityView(SQL.query("""
                        SELECT bookingProject.uuid as uuid, debitorIV.idName || '-' || cleanIdentifier(bookingProject.caption) as idName
                            FROM hs_booking_project bookingProject
                            JOIN hs_office_debitor_iv debitorIV ON debitorIV.uuid = bookingProject.debitorUuid
                        """))
                .withRestrictedViewOrderBy(SQL.expression("caption"))
                .withUpdatableColumns("version", "caption")

                .importEntityAlias("debitor", HsOfficeDebitorEntity.class, usingDefaultCase(),
                        dependsOnColumn("debitorUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)

                .importEntityAlias("debitorRel", HsOfficeRelationRbacEntity.class, usingCase(DEBITOR),
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
                    with.incomingSuperRole("debitorRel", AGENT).unassumed();
                })
                .createSubRole(ADMIN, (with) -> {
                    with.permission(UPDATE);
                })
                .createSubRole(AGENT)
                .createSubRole(TENANT, (with) -> {
                    with.outgoingSubRole("debitorRel", TENANT);
                    with.permission(SELECT);
                })

                .limitDiagramTo("project", "debitorRel", "global");
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("6-hs-booking/620-booking-project/6203-hs-booking-project-rbac");
    }
}
