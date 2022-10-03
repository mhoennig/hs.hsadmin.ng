package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.Stringify;
import net.hostsharing.hsadminng.Stringifyable;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;

import javax.persistence.*;
import java.util.UUID;

import static net.hostsharing.hsadminng.Stringify.stringify;

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

    private @Id UUID uuid;

    @ManyToOne
    @JoinColumn(name = "partneruuid")
    private HsOfficePartnerEntity partner;

    private @Column(name = "debitornumber") Integer debitorNumber;

    @ManyToOne
    @JoinColumn(name = "billingcontactuuid")
    private HsOfficeContactEntity billingContact;

    private @Column(name = "vatid") String vatId;
    private @Column(name = "vatcountrycode") String vatCountryCode;
    private @Column(name = "vatbusiness") boolean vatBusiness;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return debitorNumber.toString();
    }
}
