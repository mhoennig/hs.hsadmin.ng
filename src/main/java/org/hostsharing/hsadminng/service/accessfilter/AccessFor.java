package org.hostsharing.hsadminng.service.accessfilter;


import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessFor {
    Role[] init() default Role.NOBODY;

    Role[] update() default Role.NOBODY;

    Role[] read() default Role.NOBODY;
}

