package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelation;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacView;
import net.hostsharing.hsadminng.rbac.generator.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.util.UUID;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REFRESH;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.DEBITOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.ColumnValue.usingCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Nullable.NULLABLE;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(schema = "hs_office", name = "debitor_rv")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("Debitor")
public class HsOfficeDebitorEntity implements BaseEntity<HsOfficeDebitorEntity>, Stringifyable {

    public static final String DEBITOR_NUMBER_TAG = "D-";
    public static final String TWO_DECIMAL_DIGITS = "^([0-9]{2})$";

    private static Stringify<HsOfficeDebitorEntity> stringify =
            stringify(HsOfficeDebitorEntity.class, "debitor")
                    .withIdProp(HsOfficeDebitorEntity::toShortString)
                    .withProp(e -> ofNullable(e.getDebitorRel()).map(HsOfficeRelation::toShortString).orElse(null))
                    .withProp(HsOfficeDebitorEntity::getDefaultPrefix)
                    .quotedValues(false);

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula(
        referencedColumnName = "uuid",
        value = """
            (
                SELECT DISTINCT partner.uuid
                FROM hs_office.partner_rv partner
                JOIN hs_office.relation_rv dRel
                    ON dRel.uuid = debitorreluuid AND dRel.type = 'DEBITOR'
                JOIN hs_office.relation_rv pRel
                    ON pRel.uuid = partner.partnerRelUuid AND pRel.type = 'PARTNER'
                WHERE pRel.holderUuid = dRel.anchorUuid
            )
            """)
    @NotFound(action = NotFoundAction.IGNORE) // TODO.impl: map a simplified raw-PartnerEntity, just for the partner-number
    private HsOfficePartnerEntity partner;

    @Column(name = "debitornumbersuffix", length = 2)
    @Pattern(regexp = TWO_DECIMAL_DIGITS)
    private String debitorNumberSuffix;

    @ManyToOne(cascade = { PERSIST, MERGE, REFRESH, DETACH }, optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "debitorreluuid", nullable = false)
    private HsOfficeRelationRealEntity debitorRel;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refundbankaccountuuid")
    @NotFound(action = NotFoundAction.IGNORE)
    private HsOfficeBankAccountEntity refundBankAccount;

    @Column(name = "defaultprefix", columnDefinition = "char(3) not null")
    private String defaultPrefix;

    @Override
    public HsOfficeDebitorEntity load() {
        BaseEntity.super.load();
        if (partner != null) {
            partner.load();
        }
        debitorRel.load();
        if (refundBankAccount != null) {
            refundBankAccount.load();
        }
        return this;
    }

    private String getDebitorNumberString() {
        return ofNullable(partner)
                .filter(partner -> debitorNumberSuffix != null)
                .map(HsOfficePartnerEntity::getPartnerNumber)
                .map(Object::toString)
                .map(partnerNumber -> partnerNumber + debitorNumberSuffix)
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
                                            FROM hs_office.partner partner
                                            JOIN hs_office.relation partnerRel
                                                ON partnerRel.uuid = partner.partnerRelUUid AND partnerRel.type = 'PARTNER'
                                            JOIN hs_office.relation debitorRel
                                                ON debitorRel.anchorUuid = partnerRel.holderUuid AND debitorRel.type = 'DEBITOR'
                                            WHERE debitorRel.uuid = debitor.debitorRelUuid)
                                         || debitorNumberSuffix as idName
                        FROM hs_office.debitor AS debitor
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
                        "defaultPrefix")
                .toRole(GLOBAL, ADMIN).grantPermission(INSERT)

                .importRootEntityAliasProxy("debitorRel", HsOfficeRelationRbacEntity.class, usingCase(DEBITOR),
                        directlyFetchedByDependsOnColumn(),
                        dependsOnColumn("debitorRelUuid"))
                .createPermission(DELETE).grantedTo("debitorRel", OWNER)
                .createPermission(UPDATE).grantedTo("debitorRel", ADMIN)
                .createPermission(SELECT).grantedTo("debitorRel", TENANT)

                .importEntityAlias("refundBankAccount", HsOfficeBankAccountEntity.class, usingDefaultCase(),
                        dependsOnColumn("refundBankAccountUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NULLABLE)
                .toRole("refundBankAccount", ADMIN).grantRole("debitorRel", AGENT)
                .toRole("debitorRel", AGENT).grantRole("refundBankAccount", REFERRER)

                .importEntityAlias("partnerRel", HsOfficeRelationRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("debitorRelUuid"),
                        fetchedBySql("""
                                SELECT ${columns}
                                    FROM hs_office.relation AS partnerRel
                                    JOIN hs_office.relation AS debitorRel
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
