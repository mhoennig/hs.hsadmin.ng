package net.hostsharing.hsadminng.hs.office.coopassets;

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

import jakarta.persistence.CascadeType;
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
import java.math.BigDecimal;
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
@Table(schema = "hs_office", name = "coopassettx_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayAs("CoopAssetsTransaction")
public class HsOfficeCoopAssetsTransactionEntity implements Stringifyable, BaseEntity<HsOfficeCoopAssetsTransactionEntity> {

    private static Stringify<HsOfficeCoopAssetsTransactionEntity> stringify = stringify(HsOfficeCoopAssetsTransactionEntity.class)
            .withIdProp(HsOfficeCoopAssetsTransactionEntity::getTaggedMemberNumber)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getValueDate)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getTransactionType)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getAssetValue)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getReference)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getComment)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getRevertedAssetTx)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getReversalAssetTx)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getAdoptionAssetTx)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getTransferAssetTx)
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
    private HsOfficeCoopAssetsTransactionType transactionType;

    @Column(name = "valuedate")
    private LocalDate valueDate;

    /**
     * The signed value which directly affects the booking balance.
     *
     * <p>This means, that a DEPOSIT is always positive, a DISBURSAL is always negative,
     * but an REVERSAL can bei either positive or negative.
     * See {@link HsOfficeCoopAssetsTransactionType} for</p> more information.
     */
    @Column(name = "assetvalue")
    private BigDecimal assetValue;

    /**
     *  The booking reference.
     */
    @Column(name = "reference")
    private String reference;

    /**
     * An optional arbitrary comment.
     */
    @Column(name = "comment")
    private String comment;

    // Optionally, the UUID of the corresponding transaction for a reversal transaction.
    @OneToOne
    @JoinColumn(name = "revertedassettxuuid")
    private HsOfficeCoopAssetsTransactionEntity revertedAssetTx;

    // and the other way around
    @OneToOne(mappedBy = "revertedAssetTx", cascade = CascadeType.PERSIST)
    private HsOfficeCoopAssetsTransactionEntity reversalAssetTx;

    // Optionally, the UUID of the corresponding transaction for a transfer transaction.
    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "assetadoptiontxuuid")
    private HsOfficeCoopAssetsTransactionEntity adoptionAssetTx;

    // and the other way around
    @OneToOne(mappedBy = "adoptionAssetTx", cascade = CascadeType.PERSIST)
    private HsOfficeCoopAssetsTransactionEntity transferAssetTx;

    @Override
    public HsOfficeCoopAssetsTransactionEntity load() {
        BaseEntity.super.load();
        membership.load();
        return this;
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    public String getTaggedMemberNumber() {
        return ofNullable(membership).map(HsOfficeMembershipEntity::toShortString).orElse("M-???????");
    }

    @Override
    public String toShortString() {
        return "%s:%.3s:%+1.2f".formatted(
                getTaggedMemberNumber(),
                transactionType,
                ofNullable(assetValue).orElse(BigDecimal.ZERO));
    }

    public static RbacSpec rbac() {
        return rbacViewFor("coopAssetsTransaction", HsOfficeCoopAssetsTransactionEntity.class)
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
        rbac().generateWithBaseFileName("5-hs-office/512-coopassets/5123-hs-office-coopassets-rbac");
    }
}
