// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.service.accessfilter.Role.Nobody;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessFor {

    Class<? extends Role>[] init() default Nobody.class;

    Class<? extends Role>[] update() default Nobody.class;

    Class<? extends Role>[] read() default Nobody.class;
}
