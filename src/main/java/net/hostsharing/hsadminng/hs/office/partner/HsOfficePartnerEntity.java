package net.hostsharing.hsadminng.hs.office.partner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContact;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.rbac.rbacobject.BaseEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelation;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static jakarta.persistence.CascadeType.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_partner_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("Partner")
public class HsOfficePartnerEntity implements Stringifyable, BaseEntity<HsOfficePartnerEntity> {

    public static final String PARTNER_NUMBER_TAG = "P-";

    private static Stringify<HsOfficePartnerEntity> stringify = stringify(HsOfficePartnerEntity.class, "partner")
            .withIdProp(HsOfficePartnerEntity::toShortString)
            .withProp(p -> ofNullable(p.getPartnerRel())
                    .map(HsOfficeRelation::getHolder)
                    .map(HsOfficePersonEntity::toShortString)
                    .orElse(null))
            .withProp(p -> ofNullable(p.getPartnerRel())
                    .map(HsOfficeRelation::getContact)
                    .map(HsOfficeContact::toShortString)
                    .orElse(null))
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @Column(name = "partnernumber", columnDefinition = "numeric(5) not null")
    private Integer partnerNumber;

    @ManyToOne(cascade = { PERSIST, MERGE, REFRESH, DETACH }, optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "partnerreluuid", nullable = false)
    private HsOfficeRelationRealEntity partnerRel;

    @ManyToOne(cascade = { PERSIST, MERGE, REFRESH, DETACH }, optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "detailsuuid")
    @NotFound(action = NotFoundAction.IGNORE)
    private HsOfficePartnerDetailsEntity details;

    @Override
    public HsOfficePartnerEntity load() {
        BaseEntity.super.load();
        partnerRel.load();
        details.load();
        return this;
    }

    public String getTaggedPartnerNumber() {
        return PARTNER_NUMBER_TAG + partnerNumber;
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return getTaggedPartnerNumber();
    }

    public static RbacView rbac() {
        return rbacViewFor("partner", HsOfficePartnerEntity.class)
                .withIdentityView(SQL.projection("'P-' || partnerNumber"))
                .withUpdatableColumns("partnerRelUuid")
                .toRole("global", ADMIN).grantPermission(INSERT)

                .importRootEntityAliasProxy("partnerRel", HsOfficeRelationRbacEntity.class,
                        usingDefaultCase(),
                        directlyFetchedByDependsOnColumn(),
                        dependsOnColumn("partnerRelUuid"))
                .createPermission(DELETE).grantedTo("partnerRel", OWNER)
                .createPermission(UPDATE).grantedTo("partnerRel", ADMIN)
                .createPermission(SELECT).grantedTo("partnerRel", TENANT)

                .importSubEntityAlias("partnerDetails", HsOfficePartnerDetailsEntity.class,
                        directlyFetchedByDependsOnColumn(),
                        dependsOnColumn("detailsUuid"))
                .createPermission("partnerDetails", DELETE).grantedTo("partnerRel", OWNER)
                .createPermission("partnerDetails", UPDATE).grantedTo("partnerRel", AGENT)
                .createPermission("partnerDetails", SELECT).grantedTo("partnerRel", AGENT); // not TENANT!
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/504-partner/5043-hs-office-partner-rbac");
    }
}
