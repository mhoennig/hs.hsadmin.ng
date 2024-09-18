package net.hostsharing.hsadminng.hs.office.coopshares;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacView;
import net.hostsharing.hsadminng.rbac.object.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacView.SQL;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.ColumnValue.usingDefaultCase;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.generator.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(schema = "hs_office", name = "coopsharestransaction_rv")
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
            .withProp(at -> ofNullable(at.getAdjustedShareTx()).map(HsOfficeCoopSharesTransactionEntity::toShortString).orElse(null))
            .withProp(at -> ofNullable(at.getAdjustmentShareTx()).map(HsOfficeCoopSharesTransactionEntity::toShortString).orElse(null))
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
     * but an ADJUSTMENT can bei either positive or negative.
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
     * Optionally, the UUID of the corresponding transaction for an adjustment transaction.
     */
    @OneToOne
    @JoinColumn(name = "adjustedsharetxuuid")
    private HsOfficeCoopSharesTransactionEntity adjustedShareTx;

    @OneToOne(mappedBy = "adjustedShareTx")
    private HsOfficeCoopSharesTransactionEntity adjustmentShareTx;

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

    public static RbacView rbac() {
        return rbacViewFor("coopSharesTransaction", HsOfficeCoopSharesTransactionEntity.class)
                .withIdentityView(SQL.projection("reference"))
                .withUpdatableColumns("comment")
                .importEntityAlias("membership", HsOfficeMembershipEntity.class, usingDefaultCase(),
                        dependsOnColumn("membershipUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)

                .toRole("membership", ADMIN).grantPermission(INSERT)
                .toRole("membership", ADMIN).grantPermission(UPDATE)
                .toRole("membership", AGENT).grantPermission(SELECT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("5-hs-office/511-coopshares/5113-hs-office-coopshares-rbac");
    }
}
