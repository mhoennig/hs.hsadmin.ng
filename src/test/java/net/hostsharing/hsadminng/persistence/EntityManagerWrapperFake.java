package net.hostsharing.hsadminng.persistence;

import lombok.SneakyThrows;

import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


public class EntityManagerWrapperFake extends EntityManagerWrapper {

    private Map<Class<?>, Map<Object, Object>> entityClasses = new HashMap<>();

    @Override
    public boolean contains(final Object entity) {
        final var id = getEntityId(entity);
        return find(entity.getClass(), id) != null;
    }

    @Override
    public <T> T getReference(final Class<T> entityClass, final Object primaryKey) {
        return find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(final Class<T> entityClass, final Object primaryKey) {
        if (entityClasses.containsKey(entityClass)) {
            final var entities = entityClasses.get(entityClass);
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
        final var id = getEntityId(entity).orElseGet(() -> setEntityId(entity, UUID.randomUUID()));
        entityClasses.get(entity.getClass()).put(id, entity);
    }

    @Override
    public void flush() {
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
