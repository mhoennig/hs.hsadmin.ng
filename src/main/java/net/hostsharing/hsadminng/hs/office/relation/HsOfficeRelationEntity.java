package net.hostsharing.hsadminng.hs.office.relation;

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
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.RbacUserReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_relation_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class HsOfficeRelationEntity implements HasUuid, Stringifyable {

    private static Stringify<HsOfficeRelationEntity> toString = stringify(HsOfficeRelationEntity.class, "rel")
            .withProp(Fields.anchor, HsOfficeRelationEntity::getAnchor)
            .withProp(Fields.type, HsOfficeRelationEntity::getType)
            .withProp(Fields.mark, HsOfficeRelationEntity::getMark)
            .withProp(Fields.holder, HsOfficeRelationEntity::getHolder)
            .withProp(Fields.contact, HsOfficeRelationEntity::getContact);

    private static Stringify<HsOfficeRelationEntity> toShortString = stringify(HsOfficeRelationEntity.class, "rel")
            .withProp(Fields.anchor, HsOfficeRelationEntity::getAnchor)
            .withProp(Fields.type, HsOfficeRelationEntity::getType)
            .withProp(Fields.holder, HsOfficeRelationEntity::getHolder);

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "anchoruuid")
    private HsOfficePersonEntity anchor;

    @ManyToOne
    @JoinColumn(name = "holderuuid")
    private HsOfficePersonEntity holder;

    @ManyToOne
    @JoinColumn(name = "contactuuid")
    private HsOfficeContactEntity contact;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private HsOfficeRelationType type;

    @Column(name = "mark")
    private String mark;

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return toShortString.apply(this);
    }

    public static RbacView rbac() {
        return rbacViewFor("relation", HsOfficeRelationEntity.class)
                .withIdentityView(SQL.projection("""
                             (select idName from hs_office_person_iv p where p.uuid = anchorUuid)
                             || '-with-' || target.type || '-'
                             || (select idName from hs_office_person_iv p where p.uuid = holderUuid)
                        """))
                .withRestrictedViewOrderBy(SQL.expression(
                        "(select idName from hs_office_person_iv p where p.uuid = target.holderUuid)"))
                .withUpdatableColumns("contactUuid")
                .importEntityAlias("anchorPerson", HsOfficePersonEntity.class,
                        dependsOnColumn("anchorUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .importEntityAlias("holderPerson", HsOfficePersonEntity.class,
                        dependsOnColumn("holderUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .importEntityAlias("contact", HsOfficeContactEntity.class,
                        dependsOnColumn("contactUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)
                .createRole(OWNER, (with) -> {
                    with.owningUser(CREATOR);
                    with.incomingSuperRole(GLOBAL, ADMIN);
                    // TODO: if type=REPRESENTATIIVE
                    // with.incomingSuperRole("holderPerson", ADMIN);
                    with.permission(DELETE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.incomingSuperRole("anchorPerson", ADMIN);
                    // TODO: if type=REPRESENTATIIVE
                    // with.outgoingSuperRole("anchorPerson", OWNER);
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
                })

                .toRole("anchorPerson", ADMIN).grantPermission(INSERT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("223-hs-office-relation-rbac");
    }
}
