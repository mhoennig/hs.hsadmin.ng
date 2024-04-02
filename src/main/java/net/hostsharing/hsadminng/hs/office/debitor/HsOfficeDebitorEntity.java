package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.UUID;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REFRESH;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NULLABLE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_debitor_rv")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("Debitor")
public class HsOfficeDebitorEntity implements HasUuid, Stringifyable {

    public static final String DEBITOR_NUMBER_TAG = "D-";

    private static Stringify<HsOfficeDebitorEntity> stringify =
            stringify(HsOfficeDebitorEntity.class, "debitor")
                    .withIdProp(HsOfficeDebitorEntity::toShortString)
                    .withProp(e -> ofNullable(e.getDebitorRel()).map(HsOfficeRelationEntity::toShortString).orElse(null))
                    .withProp(HsOfficeDebitorEntity::getDefaultPrefix)
                    .quotedValues(false);

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @ManyToOne
    @JoinFormula(
        referencedColumnName = "uuid",
        value = """
            (
                SELECT DISTINCT partner.uuid
                FROM hs_office_partner_rv partner
                JOIN hs_office_relation_rv dRel
                    ON dRel.uuid = debitorreluuid AND dRel.type = 'DEBITOR'
                JOIN hs_office_relation_rv pRel
                    ON pRel.uuid = partner.partnerRelUuid AND pRel.type = 'PARTNER'
                WHERE pRel.holderUuid = dRel.anchorUuid
            )
            """)
    @NotFound(action = NotFoundAction.IGNORE)
    private HsOfficePartnerEntity partner;

    @Column(name = "debitornumbersuffix", columnDefinition = "numeric(2)")
    private Byte debitorNumberSuffix; // TODO maybe rather as a formatted String?

    @ManyToOne(cascade = { PERSIST, MERGE, REFRESH, DETACH }, optional = false)
    @JoinColumn(name = "debitorreluuid", nullable = false)
    private HsOfficeRelationEntity debitorRel;

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
        return ofNullable(partner)
                .filter(partner -> debitorNumberSuffix != null)
                .map(HsOfficePartnerEntity::getPartnerNumber)
                .map(Object::toString)
                .map(partnerNumber -> partnerNumber + String.format("%02d", debitorNumberSuffix))
                .orElse(null);
    }

    public Integer getDebitorNumber() {
        return ofNullable(getDebitorNumberString()).map(Integer::parseInt).orElse(null);
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
                        SELECT debitor.uuid AS uuid,
                                    'D-' || (SELECT partner.partnerNumber
                                            FROM hs_office_partner partner
                                            JOIN hs_office_relation partnerRel
                                                ON partnerRel.uuid = partner.partnerRelUUid AND partnerRel.type = 'PARTNER'
                                            JOIN hs_office_relation debitorRel
                                                ON debitorRel.anchorUuid = partnerRel.holderUuid AND debitorRel.type = 'DEBITOR'
                                            WHERE debitorRel.uuid = debitor.debitorRelUuid)
                                         || to_char(debitorNumberSuffix, 'fm00') as idName
                        FROM hs_office_debitor AS debitor
                        """))
                .withRestrictedViewOrderBy(SQL.projection("defaultPrefix"))
                .withUpdatableColumns(
                        "debitorRelUuid",
                        "billable",
                        "refundBankAccountUuid",
                        "vatId",
                        "vatCountryCode",
                        "vatBusiness",
                        "vatReverseCharge",
                        "defaultPrefix" /* TODO: do we want that updatable? */)
                .toRole("global", ADMIN).grantPermission(INSERT)

                .importRootEntityAliasProxy("debitorRel", HsOfficeRelationEntity.class,
                        directlyFetchedByDependsOnColumn(),
                        dependsOnColumn("debitorRelUuid"))
                .createPermission(DELETE).grantedTo("debitorRel", OWNER)
                .createPermission(UPDATE).grantedTo("debitorRel", ADMIN)
                .createPermission(SELECT).grantedTo("debitorRel", TENANT)

                .importEntityAlias("refundBankAccount", HsOfficeBankAccountEntity.class,
                        dependsOnColumn("refundBankAccountUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)
                .toRole("refundBankAccount", ADMIN).grantRole("debitorRel", AGENT)
                .toRole("debitorRel", AGENT).grantRole("refundBankAccount", REFERRER)

                .importEntityAlias("partnerRel", HsOfficeRelationEntity.class,
                        dependsOnColumn("debitorRelUuid"),
                        fetchedBySql("""
                                SELECT ${columns}
                                    FROM hs_office_relation AS partnerRel
                                    JOIN hs_office_relation AS debitorRel
                                        ON debitorRel.type = 'DEBITOR' AND debitorRel.anchorUuid = partnerRel.holderUuid
                                    WHERE partnerRel.type = 'PARTNER'
                                        AND ${REF}.debitorRelUuid = debitorRel.uuid
                                """),
                        NOT_NULL)
                .toRole("partnerRel", ADMIN).grantRole("debitorRel", ADMIN)
                .toRole("partnerRel", AGENT).grantRole("debitorRel", AGENT)
                .toRole("debitorRel", AGENT).grantRole("partnerRel", TENANT)
                .declarePlaceholderEntityAliases("partnerPerson", "operationalPerson")
                .forExampleRole("partnerPerson", ADMIN).wouldBeGrantedTo("partnerRel", ADMIN)
                .forExampleRole("operationalPerson", ADMIN).wouldBeGrantedTo("partnerRel", ADMIN)
                .forExampleRole("partnerRel", TENANT).wouldBeGrantedTo("partnerPerson", REFERRER);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/506-debitor/5063-hs-office-debitor-rbac");
    }
}
