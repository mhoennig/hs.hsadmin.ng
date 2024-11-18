package net.hostsharing.hsadminng.hs.booking.project;

import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;

import jakarta.persistence.*;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(builderMethodName = "baseBuilder", toBuilder = true)
public abstract class HsBookingProject implements Stringifyable, BaseEntity<HsBookingProject> {

    private static Stringify<HsBookingProject> stringify = stringify(HsBookingProject.class)
            .withProp(HsBookingProject::getDebitor)
            .withProp(HsBookingProject::getCaption)
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @Version
    private int version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "debitoruuid")
    private HsBookingDebitorEntity debitor;

    @Column(name = "caption")
    private String caption;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return ofNullable(debitor).map(HsBookingDebitorEntity::toShortString).orElse("D-???????") +
                ":" + caption;
    }
}
