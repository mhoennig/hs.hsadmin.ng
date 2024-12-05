package net.hostsharing.hsadminng.hs.booking.project;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingProjectRealRepository extends HsBookingProjectRepository<HsBookingProjectRealEntity>,
        Repository<HsBookingProjectRealEntity, UUID> {

    @Timed("app.bookingProjects.repo.findByUuid.real")
    Optional<HsBookingProjectRealEntity> findByUuid(final UUID bookingProjectUuid);

    @Timed("app.bookingProjects.repo.findByCaption.real")
    List<HsBookingProjectRealEntity> findByCaption(final String projectCaption);

    @Timed("app.bookingProjects.repo.findAllByDebitorUuid.real")
    List<HsBookingProjectRealEntity> findAllByDebitorUuid(final UUID bookingProjectUuid);

    @Timed("app.bookingProjects.repo.save.real")
    HsBookingProjectRealEntity save(HsBookingProjectRealEntity current);

    @Timed("app.bookingProjects.repo.deleteByUuid.real")
    int deleteByUuid(final UUID uuid);

    @Timed("app.bookingProjects.repo.count.real")
    long count();
}
