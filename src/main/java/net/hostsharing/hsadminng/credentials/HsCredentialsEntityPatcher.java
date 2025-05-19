package net.hostsharing.hsadminng.credentials;

import net.hostsharing.hsadminng.credentials.generated.api.v1.model.LoginContextResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.LoginCredentialsPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HsCredentialsEntityPatcher implements EntityPatcher<LoginCredentialsPatchResource> {

    private final EntityManager em;
    private final HsCredentialsEntity entity;

    public HsCredentialsEntityPatcher(final EntityManager em, final HsCredentialsEntity entity) {
        this.em = em;
        this.entity = entity;
    }

    @Override
    public void apply(final LoginCredentialsPatchResource resource) {
        if ( resource.getActive() != null ) {
                entity.setActive(resource.getActive());
        }
        OptionalFromJson.of(resource.getEmailAddress())
                .ifPresent(entity::setEmailAddress);
        OptionalFromJson.of(resource.getTwoFactorAuth())
                .ifPresent(entity::setTwoFactorAuth);
        OptionalFromJson.of(resource.getSmsNumber())
                .ifPresent(entity::setSmsNumber);
        OptionalFromJson.of(resource.getPhonePassword())
                .ifPresent(entity::setPhonePassword);
        if (resource.getContexts() != null) {
            syncLoginContextEntities(resource.getContexts(), entity.getLoginContexts());
        }
    }

    public void syncLoginContextEntities(
            List<LoginContextResource> resources,
            Set<HsCredentialsContextRealEntity> entities
    ) {
        final var resourceUuids = resources.stream()
                .map(LoginContextResource::getUuid)
                .collect(Collectors.toSet());

        final var entityUuids = entities.stream()
                .map(HsCredentialsContextRealEntity::getUuid)
                .collect(Collectors.toSet());

        entities.removeIf(e -> !resourceUuids.contains(e.getUuid()));

        for (final var resource : resources) {
            if (!entityUuids.contains(resource.getUuid())) {
                final var existingContextEntity = em.find(HsCredentialsContextRealEntity.class, resource.getUuid());
                if ( existingContextEntity == null ) {
                    // FIXME: i18n
                    throw new EntityNotFoundException(
                            HsCredentialsContextRealEntity.class.getName() + " with uuid " + resource.getUuid() + " not found.");
                }
                if (!existingContextEntity.getType().equals(resource.getType().name()) &&
                    !existingContextEntity.getQualifier().equals(resource.getQualifier())) {
                    // FIXME: i18n
                    throw new EntityNotFoundException("existing " +  existingContextEntity + " does not match given resource " + resource);
                }
                entities.add(existingContextEntity);
            }
        }
    }

}
