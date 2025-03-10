package net.hostsharing.hsadminng.hs.office.relation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;

import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.CaseDef.inCaseOf;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.CaseDef.inOtherCases;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.DELETE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.OWNER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.REFERRER;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.TENANT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;

@Entity
@Table(schema = "hs_office", name = "relation_rv")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@DisplayAs("RbacRelation")
public class HsOfficeRelationRbacEntity extends HsOfficeRelation {

    public static RbacSpec rbac() {
        return rbacViewFor("relation", HsOfficeRelationRbacEntity.class)
                .withIdentityView(SQL.projection("""
                             (select idName from hs_office.person_iv p where p.uuid = anchorUuid)
                             || '-with-' || target.type || '-'
                             || (select idName from hs_office.person_iv p where p.uuid = holderUuid)
                        """))
                .withRestrictedViewOrderBy(SQL.expression(
                        "(select idName from hs_office.person_iv p where p.uuid = target.holderUuid)"))
                .withUpdatableColumns("anchorUuid", "holderUuid", "contactUuid") // BEWARE: additional checks at API-level
                .importEntityAlias("anchorPerson", HsOfficePersonRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("anchorUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .importEntityAlias("holderPerson", HsOfficePersonRbacEntity.class, usingDefaultCase(),
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
