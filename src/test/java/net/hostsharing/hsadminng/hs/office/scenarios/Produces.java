package net.hostsharing.hsadminng.hs.office.scenarios;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(METHOD)
@Retention(RUNTIME)
public @interface Produces {
    String value() default ""; // same as explicitly, makes it possible to omit the property name
    String explicitly() default ""; // same as value
    String[] implicitly() default {};
    boolean permanent() default true; // false means that the object gets deleted again in the process
}
