package net.hostsharing.hsadminng.hs.office.coopshares;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_coopsharestransaction_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("CoopShareTransaction")
public class HsOfficeCoopSharesTransactionEntity implements Stringifyable {

    private static Stringify<HsOfficeCoopSharesTransactionEntity> stringify = stringify(HsOfficeCoopSharesTransactionEntity.class)
            .withProp(e -> e.getMembership().getMemberNumber())
            .withProp(HsOfficeCoopSharesTransactionEntity::getValueDate)
            .withProp(HsOfficeCoopSharesTransactionEntity::getTransactionType)
            .withProp(HsOfficeCoopSharesTransactionEntity::getShareCount)
            .withProp(HsOfficeCoopSharesTransactionEntity::getReference)
            .withSeparator(", ")
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "membershipuuid")
    private HsOfficeMembershipEntity membership;

    @Column(name = "transactiontype")
    @Enumerated(EnumType.STRING)
    @Type(PostgreSQLEnumType.class)
    private HsOfficeCoopSharesTransactionType transactionType;

    @Column(name = "valuedate")
    private LocalDate valueDate;

    @Column(name = "sharecount")
    private int shareCount;

    @Column(name = "reference")
    private String reference;

    @Column(name = "comment")
    private String comment;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return "%s%+d".formatted(membership.getMemberNumber(), shareCount);
    }
}
