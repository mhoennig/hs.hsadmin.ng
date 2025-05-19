package net.hostsharing.hsadminng.credentials;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsCredentialsContextRealRepository extends Repository<HsCredentialsContextRealEntity, UUID> {

    @Timed("app.login.context.repo.findAll")
    List<HsCredentialsContextRealEntity> findAll();

    @Timed("app.login.context.repo.findByUuid")
    Optional<HsCredentialsContextRealEntity> findByUuid(final UUID id);

    @Timed("app.login.context.repo.findByTypeAndQualifier")
    Optional<HsCredentialsContextRealEntity> findByTypeAndQualifier(@NotNull String contextType, @NotNull String qualifier);

    @Timed("app.login.context.repo.save")
    HsCredentialsContextRealEntity save(final HsCredentialsContextRealEntity entity);
}
