package net.hostsharing.hsadminng.hs.office.partner;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_partner_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("Partner")
public class HsOfficePartnerEntity implements Stringifyable, HasUuid {

    private static Stringify<HsOfficePartnerEntity> stringify = stringify(HsOfficePartnerEntity.class, "partner")
            .withProp(HsOfficePartnerEntity::getPerson)
            .withProp(HsOfficePartnerEntity::getContact)
            .withSeparator(": ")
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Column(name = "partnernumber", columnDefinition = "numeric(5) not null")
    private Integer partnerNumber;

    @ManyToOne
    @JoinColumn(name = "partnerreluuid", nullable = false)
    private HsOfficeRelationEntity partnerRel;

    // TODO: remove, is replaced by partnerRel
    @ManyToOne
    @JoinColumn(name = "personuuid", nullable = false)
    private HsOfficePersonEntity person;

    // TODO: remove, is replaced by partnerRel
    @ManyToOne
    @JoinColumn(name = "contactuuid", nullable = false)
    private HsOfficeContactEntity contact;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH }, optional = true)
    @JoinColumn(name = "detailsuuid")
    @NotFound(action = NotFoundAction.IGNORE)
    private HsOfficePartnerDetailsEntity details;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return Optional.ofNullable(person).map(HsOfficePersonEntity::toShortString).orElse("<person=null>");
    }

    public static RbacView rbac() {
        return rbacViewFor("partner", HsOfficePartnerEntity.class)
                .withIdentityView(SQL.query("""
                        SELECT partner.partnerNumber
                            || ':' || (SELECT idName FROM hs_office_person_iv p WHERE p.uuid = partner.personUuid)
                            || '-' || (SELECT idName FROM hs_office_contact_iv c WHERE c.uuid = partner.contactUuid)
                            FROM hs_office_partner AS partner
                        """))
                .withUpdatableColumns(
                        "partnerRelUuid",
                        "personUuid",
                        "contactUuid")
                .createPermission(custom("new-partner")).grantedTo("global", ADMIN)

                .importRootEntityAliasProxy("partnerRel", HsOfficeRelationEntity.class,
                        fetchedBySql("SELECT * FROM hs_office_relation AS r WHERE r.uuid = ${ref}.partnerRelUuid"),
                        dependsOnColumn("partnerRelUuid"))
                .createPermission(DELETE).grantedTo("partnerRel", ADMIN)
                .createPermission(UPDATE).grantedTo("partnerRel", AGENT)
                .createPermission(SELECT).grantedTo("partnerRel", TENANT)

                .importSubEntityAlias("partnerDetails", HsOfficePartnerDetailsEntity.class,
                        fetchedBySql("SELECT * FROM hs_office_partner_details AS d WHERE d.uuid = ${ref}.detailsUuid"),
                        dependsOnColumn("detailsUuid"))
                .createPermission("partnerDetails", DELETE).grantedTo("partnerRel", ADMIN)
                .createPermission("partnerDetails", UPDATE).grantedTo("partnerRel", AGENT)
                .createPermission("partnerDetails", SELECT).grantedTo("partnerRel", AGENT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("233-hs-office-partner-rbac-generated");
    }
}
