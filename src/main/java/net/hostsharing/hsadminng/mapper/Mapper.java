package net.hostsharing.hsadminng.mapper;

import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

import jakarta.persistence.ManyToOne;
import jakarta.validation.ValidationException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.errors.DisplayAs.DisplayName;

/**
 * A nicer API for ModelMapper.
 */
abstract class Mapper extends ModelMapper {

    EntityManagerWrapper em;

    Mapper(@Autowired final EntityManagerWrapper em) {
        this.em = em;
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
        return map("", source, destinationType);
    }

    public <D> D map(final String namePrefix, final Object source, final Class<D> destinationType) {
        final var target = super.map(source, destinationType);
        for (Field f : getDeclaredFieldsIncludingSuperClasses(destinationType)) {
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
            ReflectionUtils.setField(f, target, fetchEntity(namePrefix + f.getName() + ".uuid", f.getType(), subEntityUuid));
        }
        return target;
    }

    private static <D> Field[] getDeclaredFieldsIncludingSuperClasses(final Class<D> destinationType) {
        if (destinationType == null) {
            return new Field[0];
        }

        return Stream.concat(
                stream(destinationType.getDeclaredFields()),
                stream(getDeclaredFieldsIncludingSuperClasses(destinationType.getSuperclass())))
            .toArray(Field[]::new);
    }

    public <E> E fetchEntity(final String propertyName, final Class<E> entityClass, final Object subEntityUuid) {
        final var entity = em.getReference(entityClass, subEntityUuid);
        if (entity != null) {
            return entity;
        }
        throw new ValidationException(
                "Unable to find " + DisplayName.of(entityClass) +
                " by " + propertyName + ": " + subEntityUuid);
    }

    public <S, T> T map(final S source, final Class<T> targetClass, final BiConsumer<S, T> postMapper) {
        if (source == null) {
            return null;
        }
        final var target = map(source, targetClass);
        postMapper.accept(source, target);
        return target;
    }

    public <S, T> T map(final String namePrefix, final S source, final Class<T> targetClass, final BiConsumer<S, T> postMapper) {
        if (source == null) {
            return null;
        }
        final var target = map(source, targetClass);
        postMapper.accept(source, target);
        return target;
    }
}
