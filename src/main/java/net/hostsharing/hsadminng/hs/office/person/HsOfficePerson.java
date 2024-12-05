package net.hostsharing.hsadminng.hs.office.person;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.errors.DisplayAs;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.UUID;

import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = true)
@FieldNameConstants
@DisplayAs("Person")
public class HsOfficePerson<T extends HsOfficePerson<?> & BaseEntity<?>> implements BaseEntity<T>, Stringifyable {

    private static Stringify<HsOfficePerson> toString = stringify(HsOfficePerson.class, "person")
            .withProp(Fields.personType, HsOfficePerson::getPersonType)
            .withProp(Fields.tradeName, HsOfficePerson::getTradeName)
            .withProp(Fields.salutation, HsOfficePerson::getSalutation)
            .withProp(Fields.title, HsOfficePerson::getTitle)
            .withProp(Fields.familyName, HsOfficePerson::getFamilyName)
            .withProp(Fields.givenName, HsOfficePerson::getGivenName);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @Column(name = "persontype")
    private HsOfficePersonType personType;

    @Column(name = "tradename")
    private String tradeName;

    @Column(name = "salutation")
    private String salutation;

    @Column(name = "title")
    private String title;

    @Column(name = "familyname")
    private String familyName;

    @Column(name = "givenname")
    private String givenName;

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return personType + " " +
                (!StringUtils.isEmpty(tradeName) ? tradeName : (familyName + ", " + givenName));
    }

}
