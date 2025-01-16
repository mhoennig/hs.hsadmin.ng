package net.hostsharing.hsadminng.hs.office.partner;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContact;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePerson;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelation;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.UUID;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REFRESH;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@DisplayAs("Partner")
public class HsOfficePartner<T extends HsOfficePartner<?>> implements Stringifyable, BaseEntity<T> {

    public static final String PARTNER_NUMBER_TAG = "P-";

    protected static Stringify<HsOfficePartner> stringify = stringify(HsOfficePartner.class, "partner")
            .withIdProp(HsOfficePartner::toShortString)
            .withProp(p -> ofNullable(p.getPartnerRel())
                    .map(HsOfficeRelation::getHolder)
                    .map(HsOfficePerson::toShortString)
                    .orElse(null))
            .withProp(p -> ofNullable(p.getPartnerRel())
                    .map(HsOfficeRelation::getContact)
                    .map(HsOfficeContact::toShortString)
                    .orElse(null))
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @Column(name = "partnernumber", columnDefinition = "numeric(5) not null")
    private Integer partnerNumber;

    @ManyToOne(cascade = { PERSIST, MERGE, REFRESH, DETACH }, optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "partnerreluuid", nullable = false)
    private HsOfficeRelationRealEntity partnerRel;

    @ManyToOne(cascade = { PERSIST, MERGE, REFRESH, DETACH }, optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "detailsuuid")
    @NotFound(action = NotFoundAction.IGNORE)
    private HsOfficePartnerDetailsEntity details;

    @Override
    public T load() {
        BaseEntity.super.load();
        partnerRel.load();
        if (details != null) {
            details.load();
        }
        //noinspection unchecked
        return (T) this;
    }

    public String getTaggedPartnerNumber() {
        return PARTNER_NUMBER_TAG + partnerNumber;
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return getTaggedPartnerNumber();
    }
}
