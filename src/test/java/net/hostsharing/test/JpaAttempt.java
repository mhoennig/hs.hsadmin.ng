package net.hostsharing.test;

import junit.framework.AssertionFailedError;
import org.springframework.core.NestedExceptionUtils;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wraps the 'when' part of a DataJpaTest to improve readability of tests.
 * <p>
 * It
 * - makes sure that the SQL code is actually performed (em.flush()),
 * - if any exception is throw, it's caught and stored,
 * - makes the result available for assertions,
 * - cleans the JPA first level cache to force assertions read from the database, not just cache,
 * - offers some assertions based on the exception.
 * *
 *
 * @param <T> success result type
 */
public class JpaAttempt<T> {

    private T result = null;
    private RuntimeException exception = null;

    private String firstRootCauseMessageLineOf(final RuntimeException exception) {
        final var rootCause = NestedExceptionUtils.getRootCause(exception);
        return Optional.ofNullable(rootCause)
            .map(Throwable::getMessage)
            .map(message -> message.split("\\r|\\n|\\r\\n", 0)[0])
            .orElse(null);
    }

    public static <T> JpaAttempt<T> attempt(final EntityManager em, final Supplier<T> code) {
        return new JpaAttempt<>(em, code);
    }

    public JpaAttempt(final EntityManager em, final Supplier<T> code) {
        try {
            result = code.get();
            em.flush();
            em.clear();
        } catch (RuntimeException exc) {
            exception = exc;
        }
    }

    public boolean wasSuccessful() {
        return exception == null;
    }

    public T returnedResult() {
        return result;
    }

    public RuntimeException caughtException() {
        return exception;
    }

    @SuppressWarnings("unchecked")
    public <E extends RuntimeException> E caughtException(final Class<E> expectedExceptionClass) {
        if (expectedExceptionClass.isAssignableFrom(exception.getClass())) {
            return (E) exception;
        }
        throw new AssertionFailedError("expected " + expectedExceptionClass + " but got " + exception);
    }

    public void assertExceptionWithRootCauseMessage(
        final Class<? extends RuntimeException> expectedExceptionClass,
        final String expectedRootCauseMessage) {
        assertThat(
            firstRootCauseMessageLineOf(caughtException(expectedExceptionClass)))
            .matches(".*" + expectedRootCauseMessage + ".*");
    }
}
