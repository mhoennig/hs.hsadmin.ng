
package net.hostsharing.hsadminng.hs.office.coopassets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacView;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.io.IOException;
import java.math.BigDecimal;
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
            .withProp(at -> ofNullable(at.getAdjustedAssetTx()).map(HsOfficeCoopAssetsTransactionEntity::toShortString).orElse(null))
            .withProp(at -> ofNullable(at.getAdjustmentAssetTx()).map(HsOfficeCoopAssetsTransactionEntity::toShortString).orElse(null))
            .quotedValues(false);

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
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
     * but an ADJUSTMENT can bei either positive or negative.
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

    /**
     * Optionally, the UUID of the corresponding transaction for an adjustment transaction.
     */
    @OneToOne
    @JoinColumn(name = "adjustedassettxuuid")
    private HsOfficeCoopAssetsTransactionEntity adjustedAssetTx;

    @OneToOne(mappedBy = "adjustedAssetTx")
    private HsOfficeCoopAssetsTransactionEntity adjustmentAssetTx;

    @Override
    public HsOfficeCoopAssetsTransactionEntity load() {
        BaseEntity.super.load();
        membership.load();
        return this;
    }

    public String getTaggedMemberNumber() {
        return ofNullable(membership).map(HsOfficeMembershipEntity::toShortString).orElse("M-???????");
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return "%s:%.3s:%+1.2f".formatted(
                getTaggedMemberNumber(),
                transactionType,
                ofNullable(assetValue).orElse(BigDecimal.ZERO));
    }

    public static RbacView rbac() {
        return rbacViewFor("coopAssetsTransaction", HsOfficeCoopAssetsTransactionEntity.class)
                .withIdentityView(RbacView.SQL.projection("reference"))
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
