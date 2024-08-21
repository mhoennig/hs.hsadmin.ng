package net.hostsharing.hsadminng.hs.booking.item;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingItemRbacRepository extends HsBookingItemRepository<HsBookingItemRbacEntity>,
        Repository<HsBookingItemRbacEntity, UUID> {

    Optional<HsBookingItemRbacEntity> findByUuid(final UUID bookingItemUuid);

    List<HsBookingItemRbacEntity> findByCaption(String bookingItemCaption);

    List<HsBookingItemRbacEntity> findAllByProjectUuid(final UUID projectItemUuid);

    HsBookingItemRbacEntity save(HsBookingItemRbacEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
