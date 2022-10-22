package net.hostsharing.hsadminng.mapper;

import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A nicer API for ModelMapper.
 */
public abstract class Mapper {

    public final static ModelMapper modelMapper = new ModelMapper();

    public static <S, T> List<T> mapList(final List<S> source, final Class<T> targetClass) {
        return mapList(source, targetClass, null);
    }

    public static <S, T> List<T> mapList(final List<S> source, final Class<T> targetClass, final BiConsumer<S, T> postMapper) {
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

    public static <S, T> T map(final S source, final Class<T> targetClass) {
        return map(source, targetClass, null);
    }

    public static <S, T> T map(final S source, final Class<T> targetClass, final BiConsumer<S, T> postMapper) {
        if (source == null) {
            return null;
        }
        final var target = modelMapper.map(source, targetClass);
        if (postMapper != null) {
            postMapper.accept(source, target);
        }
        return target;
    }
}
