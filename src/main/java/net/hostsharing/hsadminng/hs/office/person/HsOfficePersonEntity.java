package net.hostsharing.hsadminng.hs.office.person;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.Stringify;
import net.hostsharing.hsadminng.Stringifyable;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.UUID;

import static net.hostsharing.hsadminng.Stringify.stringify;

@Entity
@Table(name = "hs_office_person_rv")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class HsOfficePersonEntity implements Stringifyable {

    private static Stringify<HsOfficePersonEntity> toString = stringify(HsOfficePersonEntity.class, "person")
            .withProp(Fields.personType, HsOfficePersonEntity::getPersonType)
            .withProp(Fields.tradeName, HsOfficePersonEntity::getTradeName)
            .withProp(Fields.familyName, HsOfficePersonEntity::getFamilyName)
            .withProp(Fields.givenName, HsOfficePersonEntity::getGivenName);

    private @Id UUID uuid;

    @Column(name = "persontype")
    @Enumerated(EnumType.STRING)
    @Type( type = "pgsql_enum" )
    private HsOfficePersonType personType;

    @Column(name = "tradename")
    private String tradeName;

    @Column(name = "familyname")
    private String familyName;

    @Column(name = "givenname")
    private String givenName;

    public String getDisplayName() {
        return toShortString();
    }

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return !StringUtils.isEmpty(tradeName) ? tradeName : (familyName + ", " + givenName);
    }
}
