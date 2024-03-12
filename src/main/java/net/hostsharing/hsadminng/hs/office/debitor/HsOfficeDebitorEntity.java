package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.relationship.HsOfficeRelationshipEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_debitor_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("Debitor")
public class HsOfficeDebitorEntity implements HasUuid, Stringifyable {

    public static final String DEBITOR_NUMBER_TAG = "D-";

    // TODO: I would rather like to generate something matching this example:
    //  debitor(1234500: Test AG, tes)
    // maybe remove withSepararator (always use ', ') and add withBusinessIdProp (with ': ' afterwards)?
    private static Stringify<HsOfficeDebitorEntity> stringify =
            stringify(HsOfficeDebitorEntity.class, "debitor")
                    .withProp(e -> DEBITOR_NUMBER_TAG + e.getDebitorNumber())
                    .withProp(HsOfficeDebitorEntity::getPartner)
                    .withProp(HsOfficeDebitorEntity::getDefaultPrefix)
                    .withSeparator(": ")
                    .quotedValues(false);

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "partneruuid")
    private HsOfficePartnerEntity partner;

    @Column(name = "debitornumbersuffix", columnDefinition = "numeric(2)")
    private Byte debitorNumberSuffix; // TODO maybe rather as a formatted String?

    @ManyToOne
    @JoinColumn(name = "billingcontactuuid")
    private HsOfficeContactEntity billingContact; // TODO: migrate to billingPerson

    @Column(name = "billable", nullable = false)
    private Boolean billable; // not a primitive because otherwise the default would be false

    @Column(name = "vatid")
    private String vatId;

    @Column(name = "vatcountrycode")
    private String vatCountryCode;

    @Column(name = "vatbusiness")
    private boolean vatBusiness;

    @Column(name = "vatreversecharge")
    private boolean vatReverseCharge;

    @ManyToOne
    @JoinColumn(name = "refundbankaccountuuid")
    private HsOfficeBankAccountEntity refundBankAccount;

    @Column(name = "defaultprefix", columnDefinition = "char(3) not null")
    private String defaultPrefix;

    private String getDebitorNumberString() {
        if (partner == null || partner.getPartnerNumber() == null || debitorNumberSuffix == null) {
            return null;
        }
        return partner.getPartnerNumber() + String.format("%02d", debitorNumberSuffix);
    }

    public Integer getDebitorNumber() {
        return Optional.ofNullable(getDebitorNumberString()).map(Integer::parseInt).orElse(null);
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return DEBITOR_NUMBER_TAG + getDebitorNumberString();
    }

    public static RbacView rbac() {
        return rbacViewFor("debitor", HsOfficeDebitorEntity.class)
                .withIdentityView(SQL.query("""
                            SELECT debitor.uuid,
                                        'D-' || (SELECT partner.partnerNumber
                                                FROM hs_office_partner partner
                                                JOIN hs_office_relationship partnerRel
                                                    ON partnerRel.uuid = partner.partnerRoleUUid AND partnerRel.relType = 'PARTNER'
                                                JOIN hs_office_relationship debitorRel
                                                    ON debitorRel.relAnchorUuid = partnerRel.relHolderUuid AND partnerRel.relType = 'DEBITOR'
                                                WHERE debitorRel.uuid = debitor.debitorRelUuid)
                                             || to_char(debitorNumberSuffix, 'fm00')
                                from hs_office_debitor as debitor
                        """))
                .withUpdatableColumns(
                        "debitorRel",
                        "billable",
                        "debitorUuid",
                        "refundBankAccountUuid",
                        "vatId",
                        "vatCountryCode",
                        "vatBusiness",
                        "vatReverseCharge",
                        "defaultPrefix" /* TODO: do we want that updatable? */)
                .createPermission(custom("new-debitor")).grantedTo("global", ADMIN)

                .importRootEntityAliasProxy("debitorRel", HsOfficeRelationshipEntity.class,
                        fetchedBySql("""
                                SELECT *
                                    FROM hs_office_relationship AS r
                                    WHERE r.relType = 'DEBITOR' AND r.relHolderUuid = ${REF}.debitorRelUuid
                                """),
                        dependsOnColumn("debitorRelUuid"))
                .createPermission(DELETE).grantedTo("debitorRel", OWNER)
                .createPermission(UPDATE).grantedTo("debitorRel", ADMIN)
                .createPermission(SELECT).grantedTo("debitorRel", TENANT)

                .importEntityAlias("refundBankAccount", HsOfficeBankAccountEntity.class,
                        dependsOnColumn("refundBankAccountUuid"), fetchedBySql("""
                                SELECT *
                                    FROM hs_office_relationship AS r
                                    WHERE r.relType = 'DEBITOR' AND r.relHolderUuid = ${REF}.debitorRelUuid
                                """)
                )
                .toRole("refundBankAccount", ADMIN).grantRole("debitorRel", AGENT)
                .toRole("debitorRel", AGENT).grantRole("refundBankAccount", REFERRER)

                .importEntityAlias("partnerRel", HsOfficeRelationshipEntity.class,
                        dependsOnColumn("partnerRelUuid"), fetchedBySql("""
                                SELECT *
                                     FROM hs_office_relationship AS partnerRel
                                     WHERE ${debitorRel}.relAnchorUuid = partnerRel.relHolderUuid
                                 """)
                )
                .toRole("partnerRel", ADMIN).grantRole("debitorRel", ADMIN)
                .toRole("partnerRel", AGENT).grantRole("debitorRel", AGENT)
                .toRole("debitorRel", AGENT).grantRole("partnerRel", TENANT)
                .declarePlaceholderEntityAliases("partnerPerson", "operationalPerson")
                .forExampleRole("partnerPerson", ADMIN).wouldBeGrantedTo("partnerRel", ADMIN)
                .forExampleRole("operationalPerson", ADMIN).wouldBeGrantedTo("partnerRel", ADMIN)
                .forExampleRole("partnerRel", TENANT).wouldBeGrantedTo("partnerPerson", REFERRER);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("273-hs-office-debitor-rbac-generated");
    }
}
