package net.hostsharing.hsadminng.hs.office.partner;

import lombok.*;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hs_office_partner_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsOfficePartnerEntity {

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

    public String getDisplayName() {
        return "partner(%s, %s)".formatted(person.getDisplayName(), contact.getLabel());
    }
}
