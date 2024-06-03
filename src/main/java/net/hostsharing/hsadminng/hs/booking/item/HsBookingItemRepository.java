package net.hostsharing.hsadminng.hs.booking.item;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingItemRepository extends Repository<HsBookingItemEntity, UUID> {

    List<HsBookingItemEntity> findAll();
    Optional<HsBookingItemEntity> findByUuid(final UUID bookingItemUuid);

    List<HsBookingItemEntity> findAllByProjectUuid(final UUID projectItemUuid);

    HsBookingItemEntity save(HsBookingItemEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
