package net.hostsharing.hsadminng.hs.booking.item;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingItemRealRepository extends HsBookingItemRepository<HsBookingItemRealEntity>,
        Repository<HsBookingItemRealEntity, UUID> {

    @Timed("app.bookingItems.repo.findByUuid.real")
    Optional<HsBookingItemRealEntity> findByUuid(final UUID bookingItemUuid);

    @Timed("app.bookingItems.repo.findByCaption.real")
    List<HsBookingItemRealEntity> findByCaption(String bookingItemCaption);

    @Timed("app.bookingItems.repo.findAllByProjectUuid.real")
    List<HsBookingItemRealEntity> findAllByProjectUuid(final UUID projectItemUuid);

    @Timed("app.bookingItems.repo.save.real")
    HsBookingItemRealEntity save(HsBookingItemRealEntity current);

    @Timed("app.bookingItems.repo.deleteByUuid.real")
    int deleteByUuid(final UUID uuid);

    @Timed("app.bookingItems.repo.count.real")
    long count();
}
