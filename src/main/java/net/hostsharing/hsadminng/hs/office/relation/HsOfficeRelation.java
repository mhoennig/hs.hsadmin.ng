package net.hostsharing.hsadminng.hs.office.relation;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.rbac.object.BaseEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import java.util.UUID;

import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@FieldNameConstants
public class HsOfficeRelation implements BaseEntity<HsOfficeRelation>, Stringifyable {

    private static Stringify<HsOfficeRelation> toString = stringify(HsOfficeRelation.class, "rel")
            .withProp(Fields.anchor, HsOfficeRelation::getAnchor)
            .withProp(Fields.type, HsOfficeRelation::getType)
            .withProp(Fields.mark, HsOfficeRelation::getMark)
            .withProp(Fields.holder, HsOfficeRelation::getHolder)
            .withProp(Fields.contact, HsOfficeRelation::getContact);

    private static Stringify<HsOfficeRelation> toShortString = stringify(HsOfficeRelation.class, "rel")
            .withProp(Fields.anchor, HsOfficeRelation::getAnchor)
            .withProp(Fields.type, HsOfficeRelation::getType)
            .withProp(Fields.holder, HsOfficeRelation::getHolder);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anchoruuid")
    private HsOfficePersonEntity anchor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holderuuid")
    private HsOfficePersonEntity holder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contactuuid")
    private HsOfficeContactRealEntity contact;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private HsOfficeRelationType type;

    @Column(name = "mark")
    private String mark;

    @Override
    public HsOfficeRelation load() {
        BaseEntity.super.load();
        anchor.load();
        holder.load();
        contact.load();
        return this;
    }

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return toShortString.apply(this);
    }
}
