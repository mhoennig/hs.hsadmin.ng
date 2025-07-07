package net.hostsharing.hsadminng.credentials;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.ContextResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CredentialContextResourceToEntityMapper {

    private final EntityManager em;
    private final MessageTranslator messageTranslator;

    @Autowired
    public CredentialContextResourceToEntityMapper(EntityManager em, MessageTranslator messageTranslator) {
        this.em = em;
        this.messageTranslator = messageTranslator;
    }

    public Set<HsCredentialsContextRealEntity> mapCredentialsToContextEntities(
            List<ContextResource> resources
    ) {
        final var entities = new HashSet<HsCredentialsContextRealEntity>();
        syncCredentialsContextEntities(resources, entities);
        return entities;
    }

    public void syncCredentialsContextEntities(
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
                if (existingContextEntity == null) {
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
