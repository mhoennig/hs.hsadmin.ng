package net.hostsharing.test;

import junit.framework.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wraps the 'when' part of a DataJpaTest to improve readability of tests.
 * <p>
 * It
 * <li> makes sure that the SQL code is actually performed (em.flush()),
 * <li> if any exception is throw, it's caught and stored,
 * <li> makes the result available for assertions,
 * <li> cleans the JPA first level cache to force assertions read from the database, not just cache,
 * <li> offers some assertions based on the exception.
 * </p>
 * <p>
 * To run in same transaction as caller, use the static `attempt` method,
 * to run in a new transaction, inject this class and use the instance `transacted` methods.
 * </p>
 */
@Service
public class JpaAttempt {

    @Autowired
    private final EntityManager em;

    public JpaAttempt(final EntityManager em) {
        this.em = em;
    }

    public static <T> JpaResult<T> attempt(final EntityManager em, final Supplier<T> code) {
        try {
            final var result = new JpaResult<T>(code.get(), null);
            em.flush();
            em.clear();
            return result;
        } catch (RuntimeException exc) {
            return new JpaResult<T>(null, exc);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> JpaResult<T> transacted(final Supplier<T> code) {
        return attempt(em, code);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transacted(final Runnable code) {
        attempt(em, () -> {
            code.run();
            return null;
        });
    }

    public static class JpaResult<T> {

        final T result;
        final RuntimeException exception;

        public JpaResult(final T result, final RuntimeException exception) {
            this.result = result;
            this.exception = exception;
        }

        public boolean wasSuccessful() {
            return exception == null;
        }

        public T returnedValue() {
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
            final String... expectedRootCauseMessages) {
            assertThat(wasSuccessful()).isFalse();
            final String firstRootCauseMessageLine = firstRootCauseMessageLineOf(caughtException(expectedExceptionClass));
            for (String expectedRootCauseMessage : expectedRootCauseMessages) {
                assertThat(firstRootCauseMessageLine).contains(expectedRootCauseMessage);
            }
        }

        private String firstRootCauseMessageLineOf(final RuntimeException exception) {
            final var rootCause = NestedExceptionUtils.getRootCause(exception);
            return Optional.ofNullable(rootCause)
                .map(Throwable::getMessage)
                .map(message -> message.split("\\r|\\n|\\r\\n", 0)[0])
                .orElse(null);
        }
    }

}
