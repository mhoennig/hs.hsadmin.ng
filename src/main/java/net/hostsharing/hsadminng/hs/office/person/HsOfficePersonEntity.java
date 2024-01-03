package net.hostsharing.hsadminng.hs.office.person;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.util.UUID;

import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_person_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@DisplayName("Person")
public class HsOfficePersonEntity implements Stringifyable {

    private static Stringify<HsOfficePersonEntity> toString = stringify(HsOfficePersonEntity.class, "person")
            .withProp(Fields.personType, HsOfficePersonEntity::getPersonType)
            .withProp(Fields.tradeName, HsOfficePersonEntity::getTradeName)
            .withProp(Fields.familyName, HsOfficePersonEntity::getFamilyName)
            .withProp(Fields.givenName, HsOfficePersonEntity::getGivenName);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Column(name = "persontype")
    @Enumerated(EnumType.STRING)
    //@Type(PostgreSQLEnumType.class)
    private HsOfficePersonType personType;

    @Column(name = "tradename")
    private String tradeName;

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
        return !StringUtils.isEmpty(tradeName) ? tradeName : (familyName + ", " + givenName);
    }
}
