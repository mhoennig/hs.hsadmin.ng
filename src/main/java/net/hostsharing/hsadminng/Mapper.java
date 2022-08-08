package net.hostsharing.hsadminng;

import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A nicer API for ModelMapper.
 *
 * MOst
 */
public class Mapper {
    private final static ModelMapper modelMapper = new ModelMapper();


    public static <S, T> List<T> mapList(final List<S> source, final Class<T> targetClass) {
        return source
            .stream()
            .map(element -> modelMapper.map(element, targetClass))
            .collect(Collectors.toList());
    }

    public static <S, T> T map(final S source, final Class<T> targetClass) {
        return modelMapper.map(source, targetClass);
    }
}
