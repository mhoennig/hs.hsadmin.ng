package net.hostsharing.hsadminng.hs.office.partner;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.Stringify;
import net.hostsharing.hsadminng.Stringifyable;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.Stringify.stringify;

@Entity
@Table(name = "hs_office_partner_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("Partner")
public class HsOfficePartnerEntity implements Stringifyable {

    private static Stringify<HsOfficePartnerEntity> stringify = stringify(HsOfficePartnerEntity.class, "partner")
            .withProp(HsOfficePartnerEntity::getPerson)
            .withProp(HsOfficePartnerEntity::getContact)
            .withSeparator(": ")
            .quotedValues(false);

    private @Id UUID uuid;

    @ManyToOne
    @JoinColumn(name = "personuuid")
    private HsOfficePersonEntity person;

    @ManyToOne
    @JoinColumn(name = "contactuuid")
    private HsOfficeContactEntity contact;

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
        return person.toShortString();
    }
}
