package net.hostsharing.hsadminng.reflection;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Optional.empty;

@UtilityClass
public class AnnotationFinder {

    @SneakyThrows
    public static <T extends Annotation> Optional<T> findCallerAnnotation(
            final Class<T> annotationClassToFind,
            final Class<? extends Annotation> annotationClassToStopLookup
    ) {
        for (var element : Thread.currentThread().getStackTrace()) {
            final var clazz = Class.forName(element.getClassName());
            final var method = getMethodFromStackElement(clazz, element);

            // Check if the method is annotated with the desired annotation
            if (method != null) {
                if (method.isAnnotationPresent(annotationClassToFind)) {
                    return Optional.of(method.getAnnotation(annotationClassToFind));
                } else if (method.isAnnotationPresent(annotationClassToStopLookup)) {
                    return empty();
                }
            }
        }
        return empty();
    }

    private static Method getMethodFromStackElement(Class<?> clazz, StackTraceElement element) {
        for (var method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(element.getMethodName())) {
                return method;
            }
        }
        return null;
    }
}
