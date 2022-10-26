package net.hostsharing.hsadminng.hs.office.partner;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.UUID;

import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

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

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "personuuid", nullable = false)
    private HsOfficePersonEntity person;

    @ManyToOne
    @JoinColumn(name = "contactuuid", nullable = false)
    private HsOfficeContactEntity contact;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH}, optional = true)
    @JoinColumn(name = "detailsuuid", nullable = true)
    @NotFound(action= NotFoundAction.IGNORE)
    private HsOfficePartnerDetailsEntity details;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return person.toShortString();
    }
}
