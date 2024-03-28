
package net.hostsharing.hsadminng.hs.office.coopassets;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
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
}
