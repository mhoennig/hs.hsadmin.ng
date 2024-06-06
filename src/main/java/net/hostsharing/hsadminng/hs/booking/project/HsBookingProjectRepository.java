package net.hostsharing.hsadminng.hs.booking.project;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingProjectRepository extends Repository<HsBookingProjectEntity, UUID> {

    Optional<HsBookingProjectEntity> findByUuid(final UUID bookingProjectUuid);
    List<HsBookingProjectEntity> findByCaption(final String projectCaption);

    List<HsBookingProjectEntity> findAllByDebitorUuid(final UUID bookingProjectUuid);

    HsBookingProjectEntity save(HsBookingProjectEntity current);

    int deleteByUuid(final UUID uuid);

    long count();
}
