package net.hostsharing.hsadminng.hs.booking.project;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.DEBITOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.TENANT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "hs_booking", name = "project_rv")
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
public class HsBookingProjectRbacEntity extends HsBookingProject {

    public static RbacSpec rbac() {
        return rbacViewFor("project", HsBookingProjectRbacEntity.class)
                .withIdentityView(SQL.query("""
                        SELECT bookingProject.uuid as uuid, debitorIV.idName || '-' || base.cleanIdentifier(bookingProject.caption) as idName
                            FROM hs_booking.project bookingProject
                            JOIN hs_office.debitor_iv debitorIV ON debitorIV.uuid = bookingProject.debitorUuid
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
                                    FROM hs_office.relation debitorRel
                                    JOIN hs_office.debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
                                    WHERE debitor.uuid = ${REF}.debitorUuid
                                """),
                        NOT_NULL)
                .toRole("debitorRel", ADMIN).grantPermission(INSERT)
                .toRole(GLOBAL, ADMIN).grantPermission(DELETE)

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

                .limitDiagramTo("project", "debitorRel", "rbac.global");
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("6-hs-booking/620-booking-project/6203-hs-booking-project-rbac");
    }
}
