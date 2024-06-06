package net.hostsharing.hsadminng.hs.booking.debitor;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsBookingDebitorRepository extends Repository<HsBookingDebitorEntity, UUID> {

    Optional<HsBookingDebitorEntity> findByUuid(UUID id);

    List<HsBookingDebitorEntity> findByDebitorNumber(int debitorNumber);
}
