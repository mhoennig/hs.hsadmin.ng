package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
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
public class HsOfficeDebitorEntity implements Stringifyable {

    private static Stringify<HsOfficeDebitorEntity> stringify =
            stringify(HsOfficeDebitorEntity.class, "debitor")
                    .withProp(HsOfficeDebitorEntity::getDebitorNumber)
                    .withProp(HsOfficeDebitorEntity::getPartner)
                    .withSeparator(": ")
                    .quotedValues(false);

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "partneruuid")
    private HsOfficePartnerEntity partner;

    @Column(name = "debitornumber")
    private Integer debitorNumber;

    @ManyToOne
    @JoinColumn(name = "billingcontactuuid")
    private HsOfficeContactEntity billingContact;

    @Column(name = "vatid")
    private String vatId;

    @Column(name = "vatcountrycode")
    private String vatCountryCode;

    @Column(name = "vatbusiness")
    private boolean vatBusiness;

    @ManyToOne
    @JoinColumn(name = "refundbankaccountuuid")
    private HsOfficeBankAccountEntity refundBankAccount;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return debitorNumber.toString();
    }
}
