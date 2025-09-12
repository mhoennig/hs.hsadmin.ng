package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsProfileScopeRbacRepository extends Repository<HsProfileScopeRbacEntity, UUID> {

    @Timed("app.accounts.scope.repo.findAll.rbac")
    List<HsProfileScopeRbacEntity> findAll();

    @Timed("app.accounts.scope.repo.findByUuid.rbac")
    Optional<HsProfileScopeRbacEntity> findByUuid(final UUID id);

    @Timed("app.accounts.scope.repo.findByTypeAndQualifier.rbac")
    Optional<HsProfileScopeRbacEntity> findByTypeAndQualifier(@NotNull String contextType, @NotNull String qualifier);

    @Timed("app.accounts.scope.repo.save.rbac")
    HsProfileScopeRbacEntity save(final HsProfileScopeRbacEntity entity);
}
