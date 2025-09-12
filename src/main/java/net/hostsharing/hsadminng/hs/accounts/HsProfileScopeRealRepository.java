package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsProfileScopeRealRepository extends Repository<HsProfileScopeRealEntity, UUID> {

    @Timed("app.account.scope.repo.findAll.real")
    List<HsProfileScopeRealEntity> findAll();

    @Timed("app.account.scope.repo.findByUuid.real")
    Optional<HsProfileScopeRealEntity> findByUuid(final UUID id);

    @Timed("app.account.scope.repo.findByTypeAndQualifier.real")
    Optional<HsProfileScopeRealEntity> findByTypeAndQualifier(@NotNull String type, @NotNull String qualifier);

    @Timed("app.account.scope.repo.save.real")
    HsProfileScopeRealEntity save(final HsProfileScopeRealEntity entity);
}
