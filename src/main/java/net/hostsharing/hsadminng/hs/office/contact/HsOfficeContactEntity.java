package net.hostsharing.hsadminng.hs.office.contact;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "hs_office_contact_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsOfficeContactEntity {

    private @Id UUID uuid;
    private String label;

    @Column(name = "postaladdress")
    private String postalAddress;

    @Column(name = "emailaddresses", columnDefinition = "json")
    private String emailAddresses;

    @Column(name = "phonenumbers", columnDefinition = "json")
    private String phoneNumbers;
}
