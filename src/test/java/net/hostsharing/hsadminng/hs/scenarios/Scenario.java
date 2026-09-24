package net.hostsharing.hsadminng.hs.scenarios;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Names the scenarios of the PR documents which this test verifies, e.g. "302.01".
 * One test can carry several, because a scenario of a later PR may reuse it.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Scenario {
    String[] value();
}
