package net.hostsharing.hsadminng.persistence;

import net.hostsharing.hsadminng.errors.DisplayAs.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.Entity;
import jakarta.validation.ValidationException;

@Service
public class EntityExistsValidator {

    @Autowired
    private EntityManagerWrapper em;

    public <T extends BaseEntity<T>> void validateEntityExists(final String property, final T entitySkeleton) {
        final var foundEntity = em.find(entityClass(entitySkeleton), entitySkeleton.getUuid());
        if ( foundEntity == null) {
            throw new ValidationException("Unable to find " + DisplayName.of(entitySkeleton) + " by " + property + ": " + entitySkeleton.getUuid());
        }
    }

    private static <T extends BaseEntity<T>> Class<?> entityClass(final T entityOrProxy) {
        final var entityClass = entityClass(entityOrProxy.getClass());
        if (entityClass == null) {
            throw new IllegalArgumentException("@Entity not found in superclass hierarchy of " + entityOrProxy.getClass());
        }
        return entityClass;
    }

    private static Class<?> entityClass(final Class<?> entityOrProxyClass) {
        return entityOrProxyClass.isAnnotationPresent(Entity.class)
                ? entityOrProxyClass
                : entityOrProxyClass.getSuperclass() == null
                ? null
                : entityClass(entityOrProxyClass.getSuperclass());
    }
}
