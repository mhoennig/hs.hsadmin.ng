package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.migration.HasUuid;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_debitor_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("Debitor")
public class HsOfficeDebitorEntity implements HasUuid, Stringifyable {

    // TODO: I would rather like to generate something matching this example:
    //  debitor(1234500: Test AG, tes)
    // maybe remove withSepararator (always use ', ') and add withBusinessIdProp (with ': ' afterwards)?
    private static Stringify<HsOfficeDebitorEntity> stringify =
            stringify(HsOfficeDebitorEntity.class, "debitor")
                    .withProp(HsOfficeDebitorEntity::getDebitorNumber)
                    .withProp(HsOfficeDebitorEntity::getPartner)
                    .withProp(HsOfficeDebitorEntity::getDefaultPrefix)
                    .withSeparator(": ")
                    .quotedValues(false);

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "partneruuid")
    private HsOfficePartnerEntity partner;

    @Column(name = "debitornumbersuffix", columnDefinition = "numeric(2)")
    private Byte debitorNumberSuffix; // TODO maybe rather as a formatted String?

    @ManyToOne
    @JoinColumn(name = "billingcontactuuid")
    private HsOfficeContactEntity billingContact; // TODO: migrate to billingPerson

    @Column(name = "billable", nullable = false)
    private Boolean billable; // not a primitive because otherwise the default would be false

    @Column(name = "vatid")
    private String vatId;

    @Column(name = "vatcountrycode")
    private String vatCountryCode;

    @Column(name = "vatbusiness")
    private boolean vatBusiness;

    @Column(name = "vatreversecharge")
    private boolean vatReverseCharge;

    @ManyToOne
    @JoinColumn(name = "refundbankaccountuuid")
    private HsOfficeBankAccountEntity refundBankAccount;

    @Column(name = "defaultprefix", columnDefinition = "char(3) not null")
    private String defaultPrefix;

    public String getDebitorNumberString() {
        // TODO: refactor
        if (partner.getDebitorNumberPrefix() == null ) {
            if (debitorNumberSuffix == null) {
                return null;
            }
            return String.format("%02d", debitorNumberSuffix);
        }
        if (debitorNumberSuffix == null) {
            return partner.getDebitorNumberPrefix() + "??";
        }
        return partner.getDebitorNumberPrefix() + String.format("%02d", debitorNumberSuffix);
    }

    public Integer getDebitorNumber() {
        return Optional.ofNullable(getDebitorNumberString()).map(Integer::parseInt).orElse(null);
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return getDebitorNumberString();
    }
}
