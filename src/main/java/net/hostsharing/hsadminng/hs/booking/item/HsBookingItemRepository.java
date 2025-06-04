package net.hostsharing.hsadminng.hs.booking.item;


import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Profile("!only-prod-schema")
public interface HsBookingItemRepository<E extends HsBookingItem> {

    Optional<E> findByUuid(final UUID bookingItemUuid);

    List<E> findByCaption(String bookingItemCaption);

    List<E> findAllByProjectUuid(final UUID projectItemUuid);

    E save(E current);

    int deleteByUuid(final UUID uuid);

    long count();
}
