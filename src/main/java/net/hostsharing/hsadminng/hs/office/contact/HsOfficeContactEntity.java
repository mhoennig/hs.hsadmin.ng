package net.hostsharing.hsadminng.hs.office.contact;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.migration.HasUuid;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.util.UUID;

import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_contact_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@DisplayName("Contact")
public class HsOfficeContactEntity implements Stringifyable, HasUuid {

    private static Stringify<HsOfficeContactEntity> toString = stringify(HsOfficeContactEntity.class, "contact")
            .withProp(Fields.label, HsOfficeContactEntity::getLabel)
            .withProp(Fields.emailAddresses, HsOfficeContactEntity::getEmailAddresses);


    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;
    private String label;

    @Column(name = "postaladdress")
    private String postalAddress;

    @Column(name = "emailaddresses", columnDefinition = "json")
    private String emailAddresses; // TODO: check if we can really add multiple. format: ["eins@...", "zwei@..."]

    @Column(name = "phonenumbers", columnDefinition = "json")
    private String phoneNumbers; // TODO: check if we can really add multiple. format: { "office": "+49 40 12345-10", "fax": "+49 40 12345-05" }

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return label;
    }
}
