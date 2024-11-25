package net.hostsharing.hsadminng.test;

import org.junit.jupiter.api.extension.ExtensionContext;

import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Use this JUnit Jupiter extension to ignore failing tests annotated with annotation {@link IgnoreOnFailure}.
 *
 * <p>
 *     This is useful for outside-in-TDD, if you write a high-level (e.g. Acceptance- or Scenario-Test) before
 *     you even have an implementation for that new feature.
 *     As long as no other tests breaks, it's not a real problem merging your new test and incomplete implementation.
 * </p>
 * <p>
 *     Once the test turns green, remove the annotation  {@link IgnoreOnFailure}.
 * </p>
 *
 */
// BLOG: A JUnit Jupiter extension to ignore failed acceptance tests for outside-in TDD
public class IgnoreOnFailureExtension implements InvocationInterceptor {

    /// @hidden
    @Override
    public void interceptTestMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext) throws Throwable {

        try {
            invocation.proceed();
        } catch (final Throwable throwable) {
            if (hasIgnoreOnFailureAnnotation(extensionContext)) {
                assumeThat(true).as("ignoring failed test with @" + IgnoreOnFailure.class.getSimpleName()).isFalse();
            } else {
                throw throwable;
            }
        }
    }

    private static boolean hasIgnoreOnFailureAnnotation(final ExtensionContext context) {
        final var hasIgnoreOnFailureAnnotation = context.getTestMethod()
                .map(method -> method.getAnnotation(IgnoreOnFailure.class))
                .isPresent();
        return hasIgnoreOnFailureAnnotation;
    }
}
