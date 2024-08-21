package net.hostsharing.hsadminng.hs.booking.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingProjectRepository<E extends HsBookingProject> {

    Optional<E> findByUuid(final UUID bookingProjectUuid);
    List<E> findByCaption(final String projectCaption);

    List<E> findAllByDebitorUuid(final UUID bookingProjectUuid);

    E save(E current);

    int deleteByUuid(final UUID uuid);

    long count();
}
