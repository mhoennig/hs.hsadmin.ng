package net.hostsharing.hsadminng.hs.office.membership;

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacView;
import net.hostsharing.hsadminng.rbac.generator.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static io.hypersistence.utils.hibernate.type.range.Range.emptyRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.lowerInclusiveFromPostgresDateRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.upperInclusiveFromPostgresDateRange;
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
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.TENANT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.SQL.fetchedBySql;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(schema = "hs_office", name = "membership_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("Membership")
public class HsOfficeMembershipEntity implements BaseEntity<HsOfficeMembershipEntity>, Stringifyable {

    public static final String MEMBER_NUMBER_TAG = "M-";
    public static final String TWO_DECIMAL_DIGITS = "^([0-9]{2})$";

    private static Stringify<HsOfficeMembershipEntity> stringify = stringify(HsOfficeMembershipEntity.class)
            .withProp(e -> MEMBER_NUMBER_TAG + e.getMemberNumber())
            .withProp(HsOfficeMembershipEntity::getPartner)
            .withProp(e -> e.getValidity().asString())
            .withProp(HsOfficeMembershipEntity::getStatus)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partneruuid")
    private HsOfficePartnerEntity partner;

    @Column(name = "membernumbersuffix", length = 2)
    @Pattern(regexp = TWO_DECIMAL_DIGITS)
    private String memberNumberSuffix;

    @Column(name = "validity", columnDefinition = "daterange")
    @Type(PostgreSQLRangeType.class)
    private Range<LocalDate> validity;

    @Column(name = "membershipfeebillable", nullable = false)
    private Boolean membershipFeeBillable; // not primitive to force setting the value

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private HsOfficeMembershipStatus status;

    @Override
    public HsOfficeMembershipEntity load() {
        BaseEntity.super.load();
        partner.load();
        return this;
    }

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

    public Range<LocalDate> getValidity() {
        if (validity == null) {
            validity = emptyRange(LocalDate.class);
        }
        return validity;
    }
    public Integer getMemberNumber() {
        if (partner == null || partner.getPartnerNumber() == null || memberNumberSuffix == null ) {
            return null;
        }

        return getPartner().getPartnerNumber() * 100 + Integer.parseInt(memberNumberSuffix, 10);
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return "M-" + getMemberNumber();
    }

    @PrePersist
    void init() {
        if (getStatus() == null) {
            setStatus(HsOfficeMembershipStatus.INVALID);
        }
    }

    public static RbacView rbac() {
        return rbacViewFor("membership", HsOfficeMembershipEntity.class)
                .withIdentityView(SQL.query("""
                        SELECT m.uuid AS uuid,
                                'M-' || p.partnerNumber || m.memberNumberSuffix as idName
                        FROM hs_office.membership AS m
                        JOIN hs_office.partner AS p ON p.uuid = m.partnerUuid
                        """))
                .withRestrictedViewOrderBy(SQL.projection("validity"))
                .withUpdatableColumns("validity", "membershipFeeBillable", "status")

                .importEntityAlias("partnerRel", HsOfficeRelationRbacEntity.class, usingDefaultCase(),
                        dependsOnColumn("partnerUuid"),
                        fetchedBySql("""
                                SELECT ${columns}
                                    FROM hs_office.partner AS partner
                                    JOIN hs_office.relation AS partnerRel ON partnerRel.uuid = partner.partnerRelUuid
                                    WHERE partner.uuid = ${REF}.partnerUuid
                                """),
                        NOT_NULL)
                .toRole(GLOBAL, ADMIN).grantPermission(INSERT)

                .createRole(OWNER, (with) -> {
                    with.owningUser(CREATOR);
                })
                .createSubRole(ADMIN, (with) -> {
                    with.incomingSuperRole("partnerRel", ADMIN);
                    with.permission(DELETE);
                    with.permission(UPDATE);
                })
                .createSubRole(AGENT, (with) -> {
                    with.incomingSuperRole("partnerRel", AGENT);
                    with.outgoingSubRole("partnerRel", TENANT);
                    with.permission(SELECT);
                });
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/510-membership/5103-hs-office-membership-rbac");
    }
}
