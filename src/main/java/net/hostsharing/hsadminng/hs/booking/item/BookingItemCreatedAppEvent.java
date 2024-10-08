package net.hostsharing.hsadminng.hs.booking.item;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import jakarta.validation.constraints.NotNull;

@Getter
public class BookingItemCreatedAppEvent extends ApplicationEvent {

    private BookingItemCreatedEventEntity entity;

    public BookingItemCreatedAppEvent(
            @NotNull final Object source,
            @NotNull final HsBookingItemRealEntity newBookingItem,
            final String assetJson) {
        super(source);
        this.entity = new BookingItemCreatedEventEntity(newBookingItem, assetJson);
    }
}
