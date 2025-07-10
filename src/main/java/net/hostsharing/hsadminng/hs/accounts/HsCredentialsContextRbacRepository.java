package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsCredentialsContextRbacRepository extends Repository<HsCredentialsContextRbacEntity, UUID> {

    @Timed("app.login.context.repo.findAll")
    List<HsCredentialsContextRbacEntity> findAll();

    @Timed("app.login.context.repo.findByUuid")
    Optional<HsCredentialsContextRbacEntity> findByUuid(final UUID id);

    @Timed("app.login.context.repo.findByTypeAndQualifier")
    Optional<HsCredentialsContextRbacEntity> findByTypeAndQualifier(@NotNull String contextType, @NotNull String qualifier);

    @Timed("app.login.context.repo.save")
    HsCredentialsContextRbacEntity save(final HsCredentialsContextRbacEntity entity);
}
