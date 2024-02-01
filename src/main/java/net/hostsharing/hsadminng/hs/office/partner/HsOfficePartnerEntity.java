package net.hostsharing.hsadminng.hs.office.partner;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.relationship.HsOfficeRelationshipEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.*;
import java.util.Optional;
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
public class HsOfficePartnerEntity implements Stringifyable, HasUuid {

    private static Stringify<HsOfficePartnerEntity> stringify = stringify(HsOfficePartnerEntity.class, "partner")
            .withProp(HsOfficePartnerEntity::getPerson)
            .withProp(HsOfficePartnerEntity::getContact)
            .withSeparator(": ")
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Column(name = "partnernumber", columnDefinition = "numeric(5) not null")
    private Integer partnerNumber;

    @ManyToOne
    @JoinColumn(name = "partnerroleuuid", nullable = false)
    private HsOfficeRelationshipEntity partnerRole;

    // TODO: remove, is replaced by partnerRole
    @ManyToOne
    @JoinColumn(name = "personuuid", nullable = false)
    private HsOfficePersonEntity person;

    // TODO: remove, is replaced by partnerRole
    @ManyToOne
    @JoinColumn(name = "contactuuid", nullable = false)
    private HsOfficeContactEntity contact;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH }, optional = true)
    @JoinColumn(name = "detailsuuid")
    @NotFound(action = NotFoundAction.IGNORE)
    private HsOfficePartnerDetailsEntity details;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return Optional.ofNullable(person).map(HsOfficePersonEntity::toShortString).orElse("<person=null>");
    }
}
