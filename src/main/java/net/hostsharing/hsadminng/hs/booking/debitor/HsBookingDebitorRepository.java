package net.hostsharing.hsadminng.hs.booking.debitor;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingDebitorRepository extends Repository<HsBookingDebitorEntity, UUID> {

    @Timed("app.booking.debitor.repo.findByUuid")
    Optional<HsBookingDebitorEntity> findByUuid(UUID id);

    @Timed("app.booking.debitor.repo.findByDebitorNumber")
    List<HsBookingDebitorEntity> findByDebitorNumber(int debitorNumber);
}
