package net.hostsharing.hsadminng.hs.office.relationship;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_relationship_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class HsOfficeRelationshipEntity implements HasUuid, Stringifyable {

    private static Stringify<HsOfficeRelationshipEntity> toString = stringify(HsOfficeRelationshipEntity.class, "rel")
            .withProp(Fields.relAnchor, HsOfficeRelationshipEntity::getRelAnchor)
            .withProp(Fields.relType, HsOfficeRelationshipEntity::getRelType)
            .withProp(Fields.relMark, HsOfficeRelationshipEntity::getRelMark)
            .withProp(Fields.relHolder, HsOfficeRelationshipEntity::getRelHolder)
            .withProp(Fields.contact, HsOfficeRelationshipEntity::getContact);

    private static Stringify<HsOfficeRelationshipEntity> toShortString = stringify(HsOfficeRelationshipEntity.class, "rel")
            .withProp(Fields.relAnchor, HsOfficeRelationshipEntity::getRelAnchor)
            .withProp(Fields.relType, HsOfficeRelationshipEntity::getRelType)
            .withProp(Fields.relHolder, HsOfficeRelationshipEntity::getRelHolder);

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "relanchoruuid")
    private HsOfficePersonEntity relAnchor;

    @ManyToOne
    @JoinColumn(name = "relholderuuid")
    private HsOfficePersonEntity relHolder;

    @ManyToOne
    @JoinColumn(name = "contactuuid")
    private HsOfficeContactEntity contact;

    @Column(name = "reltype")
    @Enumerated(EnumType.STRING)
    private HsOfficeRelationshipType relType;

    @Column(name = "relmark")
    private String relMark;

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return toShortString.apply(this);
    }

    public static RbacView rbac() {
        return rbacViewFor("relationship", HsOfficeRelationshipEntity.class)
                .withIdentityView(SQL.projection("""
                             (select idName from hs_office_person_iv p where p.uuid = relAnchorUuid)
                             || '-with-' || target.relType || '-'
                             || (select idName from hs_office_person_iv p where p.uuid = relHolderUuid)
                        """))
                .withRestrictedViewOrderBy(SQL.expression(
                        "(select idName from hs_office_person_iv p where p.uuid = target.relHolderUuid)"))
                .withUpdatableColumns("contactUuid")
                .importEntityAlias("anchorPerson", HsOfficePersonEntity.class,
                        dependsOnColumn("relAnchorUuid"),
                        fetchedBySql("select * from hs_office_person as p where p.uuid = ${REF}.relAnchorUuid")
                )
                .importEntityAlias("holderPerson", HsOfficePersonEntity.class,
                        dependsOnColumn("relHolderUuid"),
                        fetchedBySql("select * from hs_office_person as p where p.uuid = ${REF}.relHolderUuid")
                )
                .importEntityAlias("contact", HsOfficeContactEntity.class,
                        dependsOnColumn("contactUuid"),
                        fetchedBySql("select * from hs_office_contact as c where c.uuid = ${REF}.contactUuid")
                )
                .createRole(OWNER, (with) -> {
                    with.owningUser(CREATOR);
                    with.incomingSuperRole(GLOBAL, ADMIN);
                    with.permission(DELETE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.incomingSuperRole("anchorPerson", ADMIN);
                    with.permission(UPDATE);
                })
                .createSubRole(AGENT, (with) -> {
                    with.incomingSuperRole("holderPerson", ADMIN);
                })
                .createSubRole(TENANT, (with) -> {
                    with.incomingSuperRole("holderPerson", ADMIN);
                    with.incomingSuperRole("contact", ADMIN);
                    with.outgoingSubRole("anchorPerson", REFERRER);
                    with.outgoingSubRole("holderPerson", REFERRER);
                    with.outgoingSubRole("contact", REFERRER);
                    with.permission(SELECT);
                });
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("223-hs-office-relationship-rbac-generated");
    }
}
