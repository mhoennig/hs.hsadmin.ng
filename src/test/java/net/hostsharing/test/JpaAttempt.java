package net.hostsharing.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

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
    private TransactionTemplate transactionTemplate;

    public static <T> JpaResult<T> attempt(final EntityManager em, final Supplier<T> code) {
        try {
            final var result = JpaResult.forValue(code.get());
            em.flush();
            em.clear();
            return result;
        } catch (final RuntimeException exc) {
            return JpaResult.forException(exc);
        }
    }

    public static JpaResult<Void> attempt(final EntityManager em, final Runnable code) {
        return attempt(em, () -> {
            code.run();
            return null;
        });
    }

    public <T> JpaResult<T> transacted(final Supplier<T> code) {
        try {
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return JpaResult.forValue(
                    transactionTemplate.execute(transactionStatus -> code.get()));
        } catch (final RuntimeException exc) {
            return JpaResult.forException(exc);
        }
    }

    public JpaResult<Void> transacted(final Runnable code) {
        try {
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.execute(transactionStatus -> {
                code.run();
                return null;
            });
            return JpaResult.forVoidValue();
        } catch (final RuntimeException exc) {
            return new JpaResult<>(null, exc);
        }
    }

    public static class JpaResult<T> {

        private final T result;
        private final RuntimeException exception;

        private JpaResult(final T result, final RuntimeException exception) {
            this.result = result;
            this.exception = exception;
        }

        static JpaResult<Void> forVoidValue() {
            return new JpaResult<>(null, null);
        }

        public static <T> JpaResult<T> forValue(final T value) {
            return new JpaResult<>(value, null);
        }

        public static <T> JpaResult<T> forException(final RuntimeException exception) {
            return new JpaResult<>(null, exception);
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
            throw new AssertionError("expected " + expectedExceptionClass + " but got " + exception);
        }

        public Throwable caughtExceptionsRootCause() {
            return exception == null ? null : NestedExceptionUtils.getRootCause(exception);
        }

        public void assertExceptionWithRootCauseMessage(
                final Class<? extends RuntimeException> expectedExceptionClass,
                final String... expectedRootCauseMessages) {
            assertThat(wasSuccessful()).as("wasSuccessful").isFalse();
            final String firstRootCauseMessageLine = firstRootCauseMessageLineOf(caughtException(expectedExceptionClass));
            for (String expectedRootCauseMessage : expectedRootCauseMessages) {
                assertThat(firstRootCauseMessageLine).contains(expectedRootCauseMessage);
            }
        }

        public JpaResult<T> assumeSuccessful() {
            assumeThat(exception).as(firstRootCauseMessageLineOf(exception)).isNull();
            return this;
        }

        public JpaResult<T> assertSuccessful() {
            assertThat(exception).as(firstRootCauseMessageLineOf(exception)).isNull();
            return this;
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
