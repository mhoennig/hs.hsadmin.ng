package net.hostsharing.hsadminng.hs.booking.item;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingItemRealRepository extends HsBookingItemRepository<HsBookingItemRealEntity>,
        Repository<HsBookingItemRealEntity, UUID> {

    Optional<HsBookingItemRealEntity> findByUuid(final UUID bookingItemUuid);

    List<HsBookingItemRealEntity> findByCaption(String bookingItemCaption);

    List<HsBookingItemRealEntity> findAllByProjectUuid(final UUID projectItemUuid);

    HsBookingItemRealEntity save(HsBookingItemRealEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
