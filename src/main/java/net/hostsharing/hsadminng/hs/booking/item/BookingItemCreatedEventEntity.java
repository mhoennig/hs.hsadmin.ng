package net.hostsharing.hsadminng.hs.booking.item;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import net.hostsharing.hsadminng.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;



@Entity
@Table(schema = "hs_booking", name = "item_created_event")
@SuperBuilder(toBuilder = true)
@Getter
@ToString
@NoArgsConstructor
public class BookingItemCreatedEventEntity implements BaseEntity {
    @Id
    @Column(name="bookingitemuuid")
    private UUID uuid;

    @MapsId
    @ManyToOne(optional = false)
    @JoinColumn(name = "bookingitemuuid", nullable = false)
    private HsBookingItemRealEntity bookingItem;

    @Version
    private int version;

    @Column(name = "assetjson")
    private String assetJson;

    @Setter
    @Column(name = "statusmessage")
    private String statusMessage;

    public BookingItemCreatedEventEntity(
            @NotNull final HsBookingItemRealEntity newBookingItem,
            final String assetJson) {
        this.bookingItem = newBookingItem;
        this.assetJson = assetJson;
    }
}
