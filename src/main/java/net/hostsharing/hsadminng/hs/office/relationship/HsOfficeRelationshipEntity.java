package net.hostsharing.hsadminng.hs.office.relationship;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.util.UUID;

import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_relationship_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class HsOfficeRelationshipEntity {

    private static Stringify<HsOfficeRelationshipEntity> toString = stringify(HsOfficeRelationshipEntity.class, "rel")
            .withProp(Fields.relAnchor, HsOfficeRelationshipEntity::getRelAnchor)
            .withProp(Fields.relType, HsOfficeRelationshipEntity::getRelType)
            .withProp(Fields.relHolder, HsOfficeRelationshipEntity::getRelHolder)
            .withProp(Fields.contact, HsOfficeRelationshipEntity::getContact);

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "relanchoruuid")
    private HsOfficePersonEntity relAnchor;

    @ManyToOne
    @JoinColumn(name = "relholderuuid")
    private HsOfficePersonEntity relHolder;

    @ManyToOne
    @JoinColumn(name = "contactuuid")
    private HsOfficeContactEntity contact;

    @Column(name = "reltype")
    @Enumerated(EnumType.STRING)
    @Type(PostgreSQLEnumType.class)
    private HsOfficeRelationshipType relType;

    @Override
    public String toString() {
        return toString.apply(this);
    }
}
