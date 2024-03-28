package net.hostsharing.hsadminng.hs.office.coopshares;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_coopsharestransaction_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("CoopShareTransaction")
public class HsOfficeCoopSharesTransactionEntity implements Stringifyable, HasUuid {

    private static Stringify<HsOfficeCoopSharesTransactionEntity> stringify = stringify(HsOfficeCoopSharesTransactionEntity.class)
            .withProp(HsOfficeCoopSharesTransactionEntity::getMemberNumberTagged)
            .withProp(HsOfficeCoopSharesTransactionEntity::getValueDate)
            .withProp(HsOfficeCoopSharesTransactionEntity::getTransactionType)
            .withProp(HsOfficeCoopSharesTransactionEntity::getShareCount)
            .withProp(HsOfficeCoopSharesTransactionEntity::getReference)
            .withProp(HsOfficeCoopSharesTransactionEntity::getComment)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

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

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    private String getMemberNumberTagged() {
        return ofNullable(membership).map(HsOfficeMembershipEntity::toShortString).orElse(null);
    }

    @Override
    public String toShortString() {
        return "%s%+d".formatted(getMemberNumberTagged(), shareCount);
    }
}
