package net.hostsharing.hsadminng.hs.booking.debitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

// a partial HsOfficeDebitorEntity to reduce the number of SQL queries to load the entity
@Entity
@Table(name = "hs_booking_debitor_xv")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("BookingDebitor")
public class HsBookingDebitorEntity implements Stringifyable {

    public static final String DEBITOR_NUMBER_TAG = "D-";

    private static Stringify<HsBookingDebitorEntity> stringify =
            stringify(HsBookingDebitorEntity.class, "booking-debitor")
                    .withIdProp(HsBookingDebitorEntity::toShortString)
                    .withProp(HsBookingDebitorEntity::getDefaultPrefix)
                    .quotedValues(false);

    @Id
    private UUID uuid;

    @Column(name = "debitornumber")
    private Integer debitorNumber;

    @Column(name = "defaultprefix", columnDefinition = "char(3) not null")
    private String defaultPrefix;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return DEBITOR_NUMBER_TAG + debitorNumber;
    }
}
