package net.hostsharing.hsadminng.hs.booking.project;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingProjectRealRepository extends HsBookingProjectRepository<HsBookingProjectRealEntity>,
        Repository<HsBookingProjectRealEntity, UUID> {

    Optional<HsBookingProjectRealEntity> findByUuid(final UUID bookingProjectUuid);
    List<HsBookingProjectRealEntity> findByCaption(final String projectCaption);

    List<HsBookingProjectRealEntity> findAllByDebitorUuid(final UUID bookingProjectUuid);

    HsBookingProjectRealEntity save(HsBookingProjectRealEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
