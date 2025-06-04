package net.hostsharing.hsadminng.hs.booking.item;

import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.Repository;

import java.util.UUID;

@Profile("!only-prod-schema")
public interface BookingItemCreatedEventRepository extends Repository<BookingItemCreatedEventEntity, UUID> {

    @Timed("app.booking.items.repo.save")
    BookingItemCreatedEventEntity save(HsBookingItemRealEntity current);

    @Timed("app.booking.items.repo.findByBookingItem")
    BookingItemCreatedEventEntity findByBookingItem(HsBookingItemRealEntity newBookingItem);
}
