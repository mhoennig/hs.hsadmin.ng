package net.hostsharing.hsadminng.persistence;

import lombok.SneakyThrows;
import lombok.val;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class EntityManagerWrapperFake extends EntityManagerWrapper {

    private Map<Class<?>, Map<Object, Object>> entityClasses = new HashMap<>();

    @Override
    public boolean contains(final Object entity) {
        return getEntityId(entity)
                .map(id -> find(entity.getClass(), id) != null)
                .orElse(false);
    }

    @Override
    public <T> T getReference(final Class<T> entityClass, final Object primaryKey) {
        return find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(final Class<T> entityClass, final Object primaryKey) {
        if (entityClasses.containsKey(entityClass)) {
            val entities = entityClasses.get(entityClass);
            //noinspection unchecked
            return entities.keySet().stream()
                    .filter(key -> key.equals(primaryKey))
                    .map(key -> (T) entities.get(key))
                    .findAny()
                    .orElse(null);
        }
        return null;
    }

    @Override
    public void persist(final Object entity) {
        if (!entityClasses.containsKey(entity.getClass())) {
            entityClasses.put(entity.getClass(), new HashMap<>());
        }
        val id = getEntityId(entity).orElseGet(() -> setEntityId(entity, UUID.randomUUID()));
        entityClasses.get(entity.getClass()).put(id, entity);
    }

    @Override
    public void flush() {
    }

    @Override
    public void detach(final Object entity) {
    }

    @Override
    public void refresh(final Object entity) {
    }

    @Override
    public void clear() {
        entityClasses.clear();
    }

    // the Jakarta Persistence 3.2 overloads would reach the null delegate of the super class,
    // so they delegate to the faked form and name themselves when they cannot

    @Override
    public <T> T find(final Class<T> entityClass, final Object primaryKey, final FindOption... options) {
        return withoutOptions(options, "find", () -> find(entityClass, primaryKey));
    }

    @Override
    public <T> T find(final EntityGraph<T> entityGraph, final Object primaryKey, final FindOption... options) {
        throw unsupported("find by EntityGraph");
    }

    @Override
    public <T> T getReference(final T entity) {
        //noinspection unchecked
        return (T) getEntityId(entity)
                .map(id -> find(entity.getClass(), id))
                .orElse(null);
    }

    @Override
    public void refresh(final Object entity, final RefreshOption... options) {
        withoutOptions(options, "refresh", () -> {
            refresh(entity);
            return null;
        });
    }

    @Override
    public void lock(final Object entity, final LockModeType lockMode, final LockOption... options) {
        throw unsupported("lock");
    }

    private <T> T withoutOptions(final Object[] options, final String method, final Supplier<T> faked) {
        if (options.length > 0) {
            throw unsupported(method + " with options");
        }
        return faked.get();
    }

    private UnsupportedOperationException unsupported(final String what) {
        return new UnsupportedOperationException(getClass().getSimpleName() + " does not implement " + what);
    }

    public Stream<Object> stream() {
        return entityClasses.values().stream().flatMap(entitiesPerClass -> entitiesPerClass.values().stream());
    }

    public <T> Stream<T> stream(final Class<T> entityClass) {
        if (entityClasses.containsKey(entityClass)) {
            //noinspection unchecked
            return (Stream<T>) entityClasses.get(entityClass).values().stream();
        }
        return Stream.empty();
    }

    @SneakyThrows
    private static Optional<Object> getEntityId(final Object entity) {
        for (Class<?> currentClass = entity.getClass(); currentClass != null; currentClass = currentClass.getSuperclass()) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    return Optional.ofNullable(field.get(entity));
                }
            }
        }
        throw new IllegalArgumentException("No @Id field found in entity class: " + entity.getClass().getName());
    }

    @SneakyThrows
    private static Object setEntityId(final Object entity, final Object id) {
        for (Class<?> currentClass = entity.getClass(); currentClass != null; currentClass = currentClass.getSuperclass()) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    field.set(entity, id);
                    return id;
                }
            }
        }
        throw new IllegalArgumentException("No @Id field found in entity class: " + entity.getClass().getName());
    }
}
