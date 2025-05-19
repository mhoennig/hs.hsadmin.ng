package net.hostsharing.hsadminng.credentials;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePerson;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsCredentialsRepository extends Repository<HsCredentialsEntity, UUID> {

    @Timed("app.login.credentials.repo.findByUuid")
    Optional<HsCredentialsEntity> findByUuid(final UUID uuid);

    @Timed("app.login.credentials.repo.findByPerson")
    List<HsCredentialsEntity> findByPerson(final HsOfficePerson personUuid);

    @Timed("app.login.credentials.repo.save")
    HsCredentialsEntity save(final HsCredentialsEntity entity);
}
