package net.hostsharing.hsadminng.hs.booking.item;

import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface BookingItemCreatedEventRepository extends Repository<BookingItemCreatedEventEntity, UUID> {

    BookingItemCreatedEventEntity save(HsBookingItemRealEntity current);

    BookingItemCreatedEventEntity findByBookingItem(HsBookingItemRealEntity newBookingItem);
}
