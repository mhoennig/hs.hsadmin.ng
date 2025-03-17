package net.hostsharing.hsadminng.config;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/** Explicitly marks a REST-Controller for not requiring authorization for Swagger UI.
 *
 * @see SecurityRequirement
 */
@Target(TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface NoSecurityRequirement {
}
