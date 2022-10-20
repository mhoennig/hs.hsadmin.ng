package net.hostsharing.hsadminng.hs.office.partner;

import lombok.*;
import net.hostsharing.hsadminng.Stringify;
import net.hostsharing.hsadminng.Stringifyable;
import net.hostsharing.hsadminng.errors.DisplayName;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.Stringify.stringify;

@Entity
@Table(name = "hs_office_partner_details_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("PartnerDetails")
public class HsOfficePartnerDetailsEntity implements Stringifyable {

    private static Stringify<HsOfficePartnerDetailsEntity> stringify = stringify(
            HsOfficePartnerDetailsEntity.class,
            "partnerDetails")
            .withProp(HsOfficePartnerDetailsEntity::getRegistrationOffice)
            .withProp(HsOfficePartnerDetailsEntity::getRegistrationNumber)
            .withProp(HsOfficePartnerDetailsEntity::getBirthday)
            .withProp(HsOfficePartnerDetailsEntity::getBirthday)
            .withProp(HsOfficePartnerDetailsEntity::getDateOfDeath)
            .withSeparator(", ")
            .quotedValues(false);

    private @Id UUID uuid;

    private @Column(name = "registrationoffice") String registrationOffice;
    private @Column(name = "registrationnumber") String registrationNumber;
    private @Column(name = "birthname") String birthName;
    private @Column(name = "birthday") LocalDate birthday;
    private @Column(name = "dateofdeath") LocalDate dateOfDeath;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return registrationNumber != null ? registrationNumber
                : birthName != null ? birthName
                : birthday != null ? birthday.toString()
                : dateOfDeath != null ? dateOfDeath.toString() : "<empty details>";
    }
}
