package net.hostsharing.hsadminng.credentials;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.ContextResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.CredentialsPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HsCredentialsEntityPatcher implements EntityPatcher<CredentialsPatchResource> {

    private final EntityManager em;
    private MessageTranslator messageTranslator;
    private final HsCredentialsEntity entity;

    public HsCredentialsEntityPatcher(final EntityManager em, MessageTranslator messageTranslator, final HsCredentialsEntity entity) {
        this.em = em;
        this.messageTranslator = messageTranslator;
        this.entity = entity;
    }

    @Override
    public void apply(final CredentialsPatchResource resource) {
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
            List<ContextResource> resources,
            Set<HsCredentialsContextRealEntity> entities
    ) {
        final var resourceUuids = resources.stream()
                .map(ContextResource::getUuid)
                .collect(Collectors.toSet());

        final var entityUuids = entities.stream()
                .map(HsCredentialsContextRealEntity::getUuid)
                .collect(Collectors.toSet());

        entities.removeIf(e -> !resourceUuids.contains(e.getUuid()));

        for (final var resource : resources) {
            if (!entityUuids.contains(resource.getUuid())) {
                final var existingContextEntity = em.find(HsCredentialsContextRealEntity.class, resource.getUuid());
                if ( existingContextEntity == null ) {
                    throw new EntityNotFoundException(
                            messageTranslator.translate("{0} \"{1}\" not found or not accessible",
                                    "credentials uuid", resource.getUuid()));
                }
                if (!existingContextEntity.getType().equals(resource.getType()) &&
                    !existingContextEntity.getQualifier().equals(resource.getQualifier())) {
                    throw new EntityNotFoundException(
                            messageTranslator.translate("existing {0} does not match given resource {1}",
                                    existingContextEntity, resource));
                }
                entities.add(existingContextEntity);
            }
        }
    }

}
