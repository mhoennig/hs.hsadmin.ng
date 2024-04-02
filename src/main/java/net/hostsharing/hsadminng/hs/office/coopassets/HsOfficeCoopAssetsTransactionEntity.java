
package net.hostsharing.hsadminng.hs.office.coopassets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.rbac.rbacdef.RbacView;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Column.dependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Nullable.NOT_NULL;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.INSERT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.SELECT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Permission.UPDATE;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.ADMIN;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.Role.AGENT;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.SQL.directlyFetchedByDependsOnColumn;
import static net.hostsharing.hsadminng.rbac.rbacdef.RbacView.rbacViewFor;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_coopassetstransaction_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("CoopAssetsTransaction")
public class HsOfficeCoopAssetsTransactionEntity implements Stringifyable, HasUuid {

    private static Stringify<HsOfficeCoopAssetsTransactionEntity> stringify = stringify(HsOfficeCoopAssetsTransactionEntity.class)
            .withIdProp(HsOfficeCoopAssetsTransactionEntity::getTaggedMemberNumber)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getValueDate)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getTransactionType)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getAssetValue)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getReference)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getComment)
            .quotedValues(false);

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

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


    public String getTaggedMemberNumber() {
        return ofNullable(membership).map(HsOfficeMembershipEntity::toShortString).orElse("M-?????");
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return "%s:%+1.2f".formatted(getTaggedMemberNumber(), Optional.ofNullable(assetValue).orElse(BigDecimal.ZERO));
    }

    public static RbacView rbac() {
        return rbacViewFor("coopAssetsTransaction", HsOfficeCoopAssetsTransactionEntity.class)
                .withIdentityView(RbacView.SQL.projection("reference"))
                .withUpdatableColumns("comment")
                .importEntityAlias("membership", HsOfficeMembershipEntity.class,
                        dependsOnColumn("membershipUuid"),
                        directlyFetchedByDependsOnColumn(),
                        NOT_NULL)

                .toRole("membership", ADMIN).grantPermission(INSERT)
                .toRole("membership", ADMIN).grantPermission(UPDATE)
                .toRole("membership", AGENT).grantPermission(SELECT);
    }

    public static void main(String[] args) throws IOException {
        rbac().generateWithBaseFileName("323-hs-office-coopassets-rbac");
    }
}
