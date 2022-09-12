package net.hostsharing.hsadminng.hs.admin.partner;

import lombok.*;
import net.hostsharing.hsadminng.hs.admin.contact.HsAdminContactEntity;
import net.hostsharing.hsadminng.hs.admin.person.HsAdminPersonEntity;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hs_admin_partner_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsAdminPartnerEntity {

    private @Id UUID uuid;

    @ManyToOne
    @JoinColumn(name = "personuuid")
    private HsAdminPersonEntity person;

    @ManyToOne
    @JoinColumn(name = "contactuuid")
    private HsAdminContactEntity contact;

    private @Column(name = "registrationoffice") String registrationOffice;
    private @Column(name = "registrationnumber") String registrationNumber;
    private @Column(name = "birthname") String birthName;
    private @Column(name = "birthday") LocalDate birthday;
    private @Column(name = "dateofdeath") LocalDate dateOfDeath;

    public String getDisplayName() {
        return "partner(%s, %s)".formatted(person.getDisplayName(), contact.getLabel());
    }
}
