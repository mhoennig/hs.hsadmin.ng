
package net.hostsharing.hsadminng.hs.office.coopassets;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.hs.office.migration.HasUuid;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
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
            .withProp(HsOfficeCoopAssetsTransactionEntity::getMemberNumber)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getValueDate)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getTransactionType)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getAssetValue)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getReference)
            .withProp(HsOfficeCoopAssetsTransactionEntity::getComment)
            .withSeparator(", ")
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

    @Column(name = "assetvalue")
    private BigDecimal assetValue;

    @Column(name = "reference")
    private String reference; // TODO: what is this for?

    @Column(name = "comment")
    private String comment;


    public Integer getMemberNumber() {
        return ofNullable(membership).map(HsOfficeMembershipEntity::getMemberNumber).orElse(null);
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return "%s%+1.2f".formatted(getMemberNumber(), assetValue);
    }
}
