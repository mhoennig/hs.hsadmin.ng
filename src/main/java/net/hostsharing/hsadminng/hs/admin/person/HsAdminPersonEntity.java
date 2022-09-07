package net.hostsharing.hsadminng.hs.admin.person;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "hs_admin_person_rv")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsAdminPersonEntity {

    private @Id UUID uuid;

    @Column(name = "persontype")
    @Enumerated(EnumType.STRING)
    @Type( type = "pgsql_enum" )
    private HsAdminPersonType personType;

    @Column(name = "tradename")
    private String tradeName;

    @Column(name = "givenname")
    private String givenName;

    @Column(name = "familyname")
    private String familyName;

    public String getDisplayName() {
        return !StringUtils.isEmpty(tradeName) ? tradeName : (familyName + ", " + givenName);
    }
}
