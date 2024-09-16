package net.hostsharing.hsadminng.hs.office.relation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacView;
import net.hostsharing.hsadminng.rbac.generator.RbacView.SQL;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;

import static net.hostsharing.hsadminng.rbac.generator.RbacView.CaseDef.inCaseOf;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.CaseDef.inOtherCases;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.REFERRER;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.TENANT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.rbacViewFor;

@Entity
@Table(name = "hs_office_relation_rv")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@DisplayAs("RbacRelation")
public class HsOfficeRelationRbacEntity extends HsOfficeRelation {

    public static RbacView rbac() {
        return rbacViewFor("relation", HsOfficeRelationRbacEntity.class)
                .withIdentityView(SQL.projection("""
                             (select idName from hs_office_person_iv p where p.uuid = anchorUuid)
                             || '-with-' || target.type || '-'
                             || (select idName from hs_office_person_iv p where p.uuid = holderUuid)
                        """))
                .withRestrictedViewOrderBy(SQL.expression(
                        "(select idName from hs_office_person_iv p where p.uuid = target.holderUuid)"))
                .withUpdatableColumns("contactUuid")
                .importEntityAlias("anchorPerson", HsOfficePersonEntity.class, usingDefaultCase(),
                        dependsOnColumn("anchorUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .importEntityAlias("holderPerson", HsOfficePersonEntity.class, usingDefaultCase(),
                        dependsOnColumn("holderUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .importEntityAlias("contact", HsOfficeContactRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("contactUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .switchOnColumn(
                        "type",
                        inCaseOf("REPRESENTATIVE", then -> {
                            then.createRole(OWNER, (with) -> {
                                        with.owningUser(CREATOR);
                                        with.incomingSuperRole(GLOBAL, ADMIN);
                                        with.incomingSuperRole("holderPerson", ADMIN);
                                        with.permission(DELETE);
                                    })
                                    .createSubRole(ADMIN, (with) -> {
                                        with.outgoingSubRole("anchorPerson", OWNER);
                                        with.permission(UPDATE);
                                    })
                                    .createSubRole(AGENT, (with) -> {
                                        with.incomingSuperRole("anchorPerson", ADMIN);
                                    })
                                    .createSubRole(TENANT, (with) -> {
                                        with.incomingSuperRole("contact", ADMIN);
                                        with.outgoingSubRole("anchorPerson", REFERRER);
                                        with.outgoingSubRole("holderPerson", REFERRER);
                                        with.outgoingSubRole("contact", REFERRER);
                                        with.permission(SELECT);
                                    });
                        }),
                        // inCaseOf("DEBITOR", then -> {}), TODO.spec: needs to be defined
                        inOtherCases(then -> {
                            then.createRole(OWNER, (with) -> {
                                        with.owningUser(CREATOR);
                                        with.incomingSuperRole(GLOBAL, ADMIN);
                                        with.incomingSuperRole("anchorPerson", ADMIN);
                                        with.permission(DELETE);
                                    })
                                    .createSubRole(ADMIN, (with) -> {
                                        with.permission(UPDATE);
                                    })
                                    .createSubRole(AGENT, (with) -> {
                                        // TODO.rbac: we need relation:PROXY, to allow changing the relation contact.
                                        // the alternative would be to move this to the relation:ADMIN role,
                                        // but then the partner holder person could update the partner relation itself,
                                        // see partner entity.
                                        with.incomingSuperRole("holderPerson", ADMIN);
                                    })
                                    .createSubRole(TENANT, (with) -> {
                                        with.incomingSuperRole("contact", ADMIN);
                                        with.outgoingSubRole("anchorPerson", REFERRER);
                                        with.outgoingSubRole("holderPerson", REFERRER);
                                        with.outgoingSubRole("contact", REFERRER);
                                        with.permission(SELECT);
                                    });
                        }))
                .toRole("anchorPerson", ADMIN).grantPermission(INSERT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/503-relation/5033-hs-office-relation-rbac");
    }
}
