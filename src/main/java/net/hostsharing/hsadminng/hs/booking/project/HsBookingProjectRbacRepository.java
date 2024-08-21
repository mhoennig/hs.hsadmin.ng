package net.hostsharing.hsadminng.hs.booking.project;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingProjectRbacRepository extends HsBookingProjectRepository<HsBookingProjectRbacEntity>,
        Repository<HsBookingProjectRbacEntity, UUID> {

    Optional<HsBookingProjectRbacEntity> findByUuid(final UUID bookingProjectUuid);
    List<HsBookingProjectRbacEntity> findByCaption(final String projectCaption);

    List<HsBookingProjectRbacEntity> findAllByDebitorUuid(final UUID bookingProjectUuid);

    HsBookingProjectRbacEntity save(HsBookingProjectRbacEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
