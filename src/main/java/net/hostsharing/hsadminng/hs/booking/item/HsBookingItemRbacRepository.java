package net.hostsharing.hsadminng.hs.booking.item;

import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Profile("!only-office")
public interface HsBookingItemRbacRepository extends HsBookingItemRepository<HsBookingItemRbacEntity>,
        Repository<HsBookingItemRbacEntity, UUID> {

    @Timed("app.bookingItems.repo.findByUuid.rbac")
    Optional<HsBookingItemRbacEntity> findByUuid(final UUID bookingItemUuid);

    @Timed("app.bookingItems.repo.findByCaption.rbac")
    List<HsBookingItemRbacEntity> findByCaption(String bookingItemCaption);

    @Timed("app.bookingItems.repo.findAllByProjectUuid.rbac")
    List<HsBookingItemRbacEntity> findAllByProjectUuid(final UUID projectItemUuid);

    @Timed("app.bookingItems.repo.save.rbac")
    HsBookingItemRbacEntity save(HsBookingItemRbacEntity current);

    @Timed("app.bookingItems.repo.deleteByUuid.rbac")
    int deleteByUuid(final UUID uuid);

    @Timed("app.bookingItems.repo.count.rbac")
    long count();
}
