package net.hostsharing.hsadminng.hs.scenarios;

import lombok.experimental.UtilityClass;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;

@Target(METHOD)
@Retention(RUNTIME)
public @interface Produces {
    String value() default ""; // same as explicitly, makes it possible to omit the property name
    String explicitly() default ""; // same as value
    String[] implicitly() default {};
    boolean permanent() default true; // false means that the object gets deleted again in the process

    @UtilityClass
    final class Aggregator {

        public static Set<String> producedAliases(final Produces producesAnnot) {
            return allOf(
                    producesAnnot.value(),
                    producesAnnot.explicitly(),
                    producesAnnot.implicitly());
        }



        private Set<String> allOf(final String value, final String explicitly, final String[] implicitly) {
            final var all = new HashSet<String>();
            if (!value.isEmpty()) {
                all.add(value);
            }
            if (!explicitly.isEmpty()) {
                all.add(explicitly);
            }
            all.addAll(asList(implicitly));
            return all;
        }
    }
}
