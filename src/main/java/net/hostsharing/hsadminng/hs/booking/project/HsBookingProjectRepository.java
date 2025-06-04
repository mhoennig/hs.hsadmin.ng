package net.hostsharing.hsadminng.hs.booking.project;

import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Profile("!only-prod-schema")
public interface HsBookingProjectRepository<E extends HsBookingProject> {

    @Timed("app.booking.projects.repo.findByUuid")
    Optional<E> findByUuid(final UUID findByUuid);

    @Timed("app.booking.projects.repo.findByCaption")
    List<E> findByCaption(final String projectCaption);

    @Timed("app.booking.projects.repo.findAllByDebitorUuid")
    List<E> findAllByDebitorUuid(final UUID bookingProjectUuid);

    @Timed("app.booking.projects.repo.save")
    E save(E current);

    @Timed("app.booking.projects.repo.deleteByUuid")
    int deleteByUuid(final UUID uuid);

    @Timed("app.booking.projects.repo.count")
    long count();
}
