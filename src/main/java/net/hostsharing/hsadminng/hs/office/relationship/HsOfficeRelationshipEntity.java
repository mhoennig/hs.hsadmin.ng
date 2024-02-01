package net.hostsharing.hsadminng.hs.office.relationship;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.persistence.HasUuid;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

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
public class HsOfficeRelationshipEntity implements HasUuid, Stringifyable {

    private static Stringify<HsOfficeRelationshipEntity> toString = stringify(HsOfficeRelationshipEntity.class, "rel")
            .withProp(Fields.relAnchor, HsOfficeRelationshipEntity::getRelAnchor)
            .withProp(Fields.relType, HsOfficeRelationshipEntity::getRelType)
            .withProp(Fields.relMark, HsOfficeRelationshipEntity::getRelMark)
            .withProp(Fields.relHolder, HsOfficeRelationshipEntity::getRelHolder)
            .withProp(Fields.contact, HsOfficeRelationshipEntity::getContact);

    private static Stringify<HsOfficeRelationshipEntity> toShortString = stringify(HsOfficeRelationshipEntity.class, "rel")
            .withProp(Fields.relAnchor, HsOfficeRelationshipEntity::getRelAnchor)
            .withProp(Fields.relType, HsOfficeRelationshipEntity::getRelType)
            .withProp(Fields.relHolder, HsOfficeRelationshipEntity::getRelHolder);

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
    private HsOfficeRelationshipType relType;

    @Column(name = "relmark")
    private String relMark;

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return toShortString.apply(this);
    }
}
