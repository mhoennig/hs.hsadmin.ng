package net.hostsharing.hsadminng.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.util.ReflectionUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ValidationException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static net.hostsharing.hsadminng.errors.DisplayAs.DisplayName;

/**
 * A nicer API for ModelMapper.
 */
public class Mapper extends ModelMapper {

    @PersistenceContext
    EntityManager em;

    public Mapper() {
        getConfiguration().setAmbiguityIgnored(true);
    }

    public <S, T> List<T> mapList(final List<S> source, final Class<T> targetClass) {
        return mapList(source, targetClass, null);
    }

    public <S, T> List<T> mapList(final List<S> source, final Class<T> targetClass, final BiConsumer<S, T> postMapper) {
        return source
                .stream()
                .map(element -> {
                    final var target = map(element, targetClass);
                    if (postMapper != null) {
                        postMapper.accept(element, target);
                    }
                    return target;
                })
                .collect(Collectors.toList());
    }

    @Override
    public <D> D map(final Object source, final Class<D> destinationType) {
        final var target = super.map(source, destinationType);
        for (Field f : destinationType.getDeclaredFields()) {
            if (f.getAnnotation(ManyToOne.class) == null) {
                continue;
            }
            ReflectionUtils.makeAccessible(f);
            final var subEntity = ReflectionUtils.getField(f, target);
            if (subEntity == null) {
                continue;
            }
            final var subEntityUuidField = ReflectionUtils.findField(f.getType(), "uuid");
            if (subEntityUuidField == null) {
                continue;
            }
            ReflectionUtils.makeAccessible(subEntityUuidField);
            final var subEntityUuid = ReflectionUtils.getField(subEntityUuidField, subEntity);
            if (subEntityUuid == null) {
                continue;
            }
            ReflectionUtils.setField(f, target, findEntityById(f.getType(), subEntityUuid));
        }
        return target;
    }

    private Object findEntityById(final Class<?> entityClass, final Object subEntityUuid) {
        // using getReference would be more efficent, but results in very technical error messages
        final var entity = em.find(entityClass, subEntityUuid);
        if (entity != null) {
            return entity;
        }
        throw new ValidationException("Unable to find " + DisplayName.of(entityClass) + " by uuid: " + subEntityUuid);
    }

    public <S, T> T map(final S source, final Class<T> targetClass, final BiConsumer<S, T> postMapper) {
        if (source == null) {
            return null;
        }
        final var target = map(source, targetClass);
        postMapper.accept(source, target);
        return target;
    }
}
