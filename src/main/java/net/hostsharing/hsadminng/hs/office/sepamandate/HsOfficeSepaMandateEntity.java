package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.DEBITOR;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.GLOBAL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacSubjectReference.UserRole.CREATOR;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.*;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@Entity
@Table(schema = "hs_office", name = "sepamandate_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("SEPA-Mandate")
public class HsOfficeSepaMandateEntity implements Stringifyable, BaseEntity<HsOfficeSepaMandateEntity> {

    private static Stringify<HsOfficeSepaMandateEntity> stringify = stringify(HsOfficeSepaMandateEntity.class)
            .withProp(e -> e.getBankAccount().getIban())
            .withProp(HsOfficeSepaMandateEntity::getReference)
            .withProp(HsOfficeSepaMandateEntity::getAgreement)
            .withProp(e -> e.getValidity().asString())
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne
    @JoinColumn(name = "debitoruuid")
    private HsOfficeDebitorEntity debitor;

    @ManyToOne
    @JoinColumn(name = "bankaccountuuid")
    private HsOfficeBankAccountEntity bankAccount;

    private @Column(name = "reference") String reference;

    @Column(name="agreement", columnDefinition = "date")
    private LocalDate agreement;

    @Column(name = "validity", columnDefinition = "daterange")
    @Type(PostgreSQLRangeType.class)
    @Builder.Default
    private Range<LocalDate> validity = Range.infinite(LocalDate.class);

    public void setValidFrom(final LocalDate validFrom) {
        setValidity(toPostgresDateRange(validFrom, getValidTo()));
    }

    public void setValidTo(final LocalDate validTo) {
        setValidity(toPostgresDateRange(getValidFrom(), validTo));
    }

    public LocalDate getValidFrom() {
        return lowerInclusiveFromPostgresDateRange(getValidity());
    }

    public LocalDate getValidTo() {
        return upperInclusiveFromPostgresDateRange(getValidity());
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return reference;
    }

    public static RbacSpec rbac() {
        return rbacViewFor("sepaMandate", HsOfficeSepaMandateEntity.class)
                .withIdentityView(query("""
                        select sm.uuid as uuid, ba.iban || '-' || sm.validity as idName
                            from hs_office.sepamandate sm
                            join hs_office.bankaccount ba on ba.uuid = sm.bankAccountUuid
                        """))
                .withRestrictedViewOrderBy(expression("validity"))
                .withUpdatableColumns("reference", "agreement", "validity")

                .importEntityAlias("debitorRel", HsOfficeRelationRbacEntity.class, usingCase(DEBITOR),
                        dependsOnColumn("debitorUuid"),
                        fetchedBySql("""
                                SELECT ${columns}
                                    FROM hs_office.relation debitorRel
                                    JOIN hs_office.debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
                                    WHERE debitor.uuid = ${REF}.debitorUuid
                                """),
                        NOT_NULL)
                .importEntityAlias("bankAccount", HsOfficeBankAccountEntity.class, usingDefaultCase(),
                        dependsOnColumn("bankAccountUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)

                .createRole(OWNER, (with) -> {
                    with.owningUser(CREATOR);
                    with.incomingSuperRole(GLOBAL, ADMIN);
                    with.permission(DELETE);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.permission(UPDATE);
                })
                .createSubRole(AGENT, (with) -> {
                    with.outgoingSubRole("bankAccount", REFERRER);
                    with.outgoingSubRole("debitorRel", AGENT);
                })
                .createSubRole(REFERRER, (with) -> {
                    with.incomingSuperRole("bankAccount", ADMIN);
                    with.incomingSuperRole("debitorRel", AGENT);
                    with.outgoingSubRole("debitorRel", TENANT);
                    with.permission(SELECT);
                })

                .toRole("debitorRel", ADMIN).grantPermission(INSERT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/507-sepamandate/5073-hs-office-sepamandate-rbac");
    }
}
