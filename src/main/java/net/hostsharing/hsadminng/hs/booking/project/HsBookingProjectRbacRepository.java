package net.hostsharing.hsadminng.hs.booking.project;

import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Profile("!only-office")
public interface HsBookingProjectRbacRepository extends HsBookingProjectRepository<HsBookingProjectRbacEntity>,
        Repository<HsBookingProjectRbacEntity, UUID> {

    @Timed("app.bookingProjects.repo.findByUuid.rbac")
    Optional<HsBookingProjectRbacEntity> findByUuid(final UUID bookingProjectUuid);

    @Timed("app.bookingProjects.repo.findByCaption.rbac")
    List<HsBookingProjectRbacEntity> findByCaption(final String projectCaption);

    @Timed("app.bookingProjects.repo.findAllByDebitorUuid.rbac")
    List<HsBookingProjectRbacEntity> findAllByDebitorUuid(final UUID bookingProjectUuid);

    @Timed("app.bookingProjects.repo.save.rbac")
    HsBookingProjectRbacEntity save(HsBookingProjectRbacEntity current);

    @Timed("app.bookingProjects.repo.deleteByUuid.rbac")
    int deleteByUuid(final UUID uuid);

    @Timed("app.bookingProjects.repo.count.rbac")
    long count();
}
