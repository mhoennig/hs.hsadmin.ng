package net.hostsharing.hsadminng.hs.office.coopshares;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec;
import net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacSpec.rbacViewFor;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@Entity
@Table(schema = "hs_office", name = "coopsharetx_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("CoopShareTransaction")
public class HsOfficeCoopSharesTransactionEntity implements Stringifyable, BaseEntity<HsOfficeCoopSharesTransactionEntity> {

    private static Stringify<HsOfficeCoopSharesTransactionEntity> stringify = stringify(HsOfficeCoopSharesTransactionEntity.class)
            .withIdProp(HsOfficeCoopSharesTransactionEntity::getMemberNumberTagged)
            .withProp(HsOfficeCoopSharesTransactionEntity::getValueDate)
            .withProp(HsOfficeCoopSharesTransactionEntity::getTransactionType)
            .withProp(HsOfficeCoopSharesTransactionEntity::getShareCount)
            .withProp(HsOfficeCoopSharesTransactionEntity::getReference)
            .withProp(HsOfficeCoopSharesTransactionEntity::getComment)
            .withProp(at -> ofNullable(at.getRevertedShareTx()).map(HsOfficeCoopSharesTransactionEntity::toShortString).orElse(null))
            .withProp(at -> ofNullable(at.getReversalShareTx()).map(HsOfficeCoopSharesTransactionEntity::toShortString).orElse(null))
        .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne
    @JoinColumn(name = "membershipuuid")
    private HsOfficeMembershipEntity membership;

    @Column(name = "transactiontype")
    @Enumerated(EnumType.STRING)
    private HsOfficeCoopSharesTransactionType transactionType;

    /**
     * The signed value which directly affects the booking balance.
     *
     * <p>This means, that a SUBSCRIPTION is always positive, a CANCELLATION is always negative,
     * but an REVERSAL can bei either positive or negative.
     * See {@link HsOfficeCoopSharesTransactionType} for</p> more information.
     */
    @Column(name = "valuedate")
    private LocalDate valueDate;

    @Column(name = "sharecount")
    private int shareCount;

    /**
     * The Booking reference.
     */
    @Column(name = "reference")
    private String reference;

    /**
     * An optional arbitrary comment.
     */
    @Column(name = "comment")
    private String comment;

    /**
     * Optionally, the UUID of the corresponding transaction for a REVERSAL transaction.
     */
    @OneToOne
    @JoinColumn(name = "revertedsharetxuuid")
    private HsOfficeCoopSharesTransactionEntity revertedShareTx;

    @OneToOne(mappedBy = "revertedShareTx")
    private HsOfficeCoopSharesTransactionEntity reversalShareTx;

    @Override
    public HsOfficeCoopSharesTransactionEntity load() {
        BaseEntity.super.load();
        membership.load();
        return this;
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    private String getMemberNumberTagged() {
        return ofNullable(membership).map(HsOfficeMembershipEntity::toShortString).orElse(null);
    }

    @Override
    public String toShortString() {
        return "%s:%.3s:%+d".formatted(getMemberNumberTagged(), transactionType, shareCount);
    }

    public static RbacSpec rbac() {
        return rbacViewFor("coopSharesTransaction", HsOfficeCoopSharesTransactionEntity.class)
                .withIdentityView(SQL.projection("reference"))
                .withUpdatableColumns("comment")
                .importEntityAlias("membership", HsOfficeMembershipEntity.class, usingDefaultCase(),
                        dependsOnColumn("membershipUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)

                // the membership:ADMIN is not to be confused with the member itself, it's an account manager of the coop
                .toRole("membership", ADMIN).grantPermission(INSERT)
                .toRole("membership", ADMIN).grantPermission(UPDATE)
                .toRole("membership", AGENT).grantPermission(SELECT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/511-coopshares/5113-hs-office-coopshares-rbac");
    }
}
