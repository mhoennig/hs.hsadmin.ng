package net.hostsharing.hsadminng.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on JUnit Jupiter test-methods to convert failure to ignore.
 *
 * <p>
 *     The test-class also has to add the extension {link IgnoreOnFailureExtension}.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreOnFailure {
    ///  a comment, e.g. about the feature under construction
    String value() default "";
}
