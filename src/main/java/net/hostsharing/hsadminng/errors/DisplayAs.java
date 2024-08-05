package net.hostsharing.hsadminng.errors;

import jakarta.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DisplayAs {
    class DisplayName {
        public static String of(final Class<?> clazz) {
            final var displayNameAnnot = clazz.getAnnotation(DisplayAs.class);
            return displayNameAnnot != null ? displayNameAnnot.value() : clazz.getSimpleName();
        }

        public static String of(@NotNull final Object instance) {
            return of(instance.getClass());
        }
    }

    String value() default "";
}
