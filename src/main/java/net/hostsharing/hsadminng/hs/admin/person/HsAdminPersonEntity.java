package net.hostsharing.hsadminng.hs.admin.person;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import lombok.*;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "hs_admin_person_rv")
@TypeDef(
        name = "list-array",
        typeClass = ListArrayType.class
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsAdminPersonEntity {

    private @Id UUID uuid;

    @Enumerated(EnumType.STRING)
    private PersonType type;

    private String tradeName;

    @Column(name = "givenname")
    private String givenName;

    @Column(name = "familyname")
    private String familyName;

    public enum PersonType {
        NATURAL,
        LEGAL,
        SOLE_REPRESENTATION,
        JOINT_REPRESENTATION
    }
}
