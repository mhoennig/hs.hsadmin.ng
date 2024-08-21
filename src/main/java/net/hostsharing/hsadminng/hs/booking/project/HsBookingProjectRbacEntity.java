package net.hostsharing.hsadminng.hs.booking.project;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.DEBITOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingCase;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingDefaultCase;
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

@Entity
@Table(name = "hs_booking_project_rv")
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
public class HsBookingProjectRbacEntity extends HsBookingProject {

    public static RbacView rbac() {
        return rbacViewFor("project", HsBookingProjectRbacEntity.class)
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
