package net.hostsharing.hsadminng.hs.booking.item;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import jakarta.validation.constraints.NotNull;

@Getter
public class BookingItemCreatedEvent extends ApplicationEvent {
    private final @NotNull HsBookingItem newBookingItem;

    public BookingItemCreatedEvent(@NotNull HsBookingItemController source, @NotNull final HsBookingItem newBookingItem) {
        super(source);
        this.newBookingItem = newBookingItem;
    }
}
