package net.hostsharing.hsadminng.hs.accounts;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ScopeResource;
import net.hostsharing.hsadminng.config.MessageTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScopeResourceToEntityMapper {

    private final EntityManager em;
    private final MessageTranslator messageTranslator;

    @Autowired
    public ScopeResourceToEntityMapper(final EntityManager em, final MessageTranslator messageTranslator) {
        this.em = em;
        this.messageTranslator = messageTranslator;
    }

    public Set<HsProfileScopeRealEntity> mapProfileToScopeEntities(
            final List<ScopeResource> resources
    ) {
        final var entities = new HashSet<HsProfileScopeRealEntity>();
        syncProfileScopeEntities(resources, entities);
        return entities;
    }

    public void syncProfileScopeEntities(
            final List<ScopeResource> resources,
            final Set<HsProfileScopeRealEntity> entities
    ) {
        final var resourceUuids = resources.stream()
                .map(ScopeResource::getUuid)
                .collect(Collectors.toSet());

        final var entityUuids = entities.stream()
                .map(HsProfileScopeRealEntity::getUuid)
                .collect(Collectors.toSet());

        entities.removeIf(e -> !resourceUuids.contains(e.getUuid()));

        for (final var resource : resources) {
            if (!entityUuids.contains(resource.getUuid())) {
                final var existingScopeEntity = em.find(HsProfileScopeRealEntity.class, resource.getUuid());
                if (existingScopeEntity == null) {
                    throw new EntityNotFoundException(
                            messageTranslator.translate(
                                    "general.{0}-{1}-not-found-or-not-accessible",
                                    "profile uuid", resource.getUuid()));
                }
                if ((resource.getType() != null && !existingScopeEntity.getType().equals(resource.getType())) ||
                    (resource.getQualifier() != null && !existingScopeEntity.getQualifier().equals(resource.getQualifier()))) {
                    throw new EntityNotFoundException(
                            messageTranslator.translate(
                                    "profile.existing-profile-scope-{0}-does-not-match-given-resource-{1}",
                                    existingScopeEntity, resource));
                }
                entities.add(existingScopeEntity);
            }
        }
    }
}
