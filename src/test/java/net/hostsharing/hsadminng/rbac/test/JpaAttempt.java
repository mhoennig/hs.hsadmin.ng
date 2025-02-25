package net.hostsharing.hsadminng.rbac.test;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
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

        private final T value;
        private final Throwable exception;

        private JpaResult(final T value, final Throwable exception) {
            this.value = value;
            this.exception = exception;
        }

        static JpaResult<Void> forVoidValue() {
            return new JpaResult<>(null, null);
        }

        public static <T> JpaResult<T> forValue(final T value) {
            return new JpaResult<>(value, null);
        }

        public static <T> JpaResult<T> forException(final Throwable exception) {
            return new JpaResult<>(null, exception);
        }

        public boolean wasSuccessful() {
            return exception == null;
        }

        public T returnedValue() {
            return value;
        }

        public Throwable caughtException() {
            return exception;
        }

        public <E extends Throwable> E caughtException(final Class<E> expectedExceptionClass) {
            //noinspection unchecked
            return caughtException((E) exception, expectedExceptionClass);
        }

        public static <E extends Throwable> E caughtException(final Throwable exception, final Class<E> expectedExceptionClass) {
            if (expectedExceptionClass.isAssignableFrom(exception.getClass())) {
                //noinspection unchecked
                return (E) exception;
            }
            if(exception.getCause() != null && exception.getCause() != exception ) {
                return caughtException(exception.getCause(), expectedExceptionClass);
            }
            throw new AssertionError("expected " + expectedExceptionClass + " but got " + exception);
        }

        public Throwable caughtExceptionsRootCause() {
            return exception == null ? null : NestedExceptionUtils.getRootCause(exception);
        }

        public void assertExceptionWithRootCauseMessage(
                final Class<? extends Throwable> expectedExceptionClass,
                final String... expectedRootCauseMessages) {
            assertThat(wasSuccessful()).as("wasSuccessful").isFalse();
            final String firstRootCauseMessageLine = firstRootCauseMessageLineOf(caughtException(expectedExceptionClass));
            for (String expectedRootCauseMessage : expectedRootCauseMessages) {
                assertThat(firstRootCauseMessageLine).contains(expectedRootCauseMessage);
            }
        }

        @SneakyThrows
        public void reThrowException() {
            if (exception != null) {
                throw exception;
            }
        }

        public JpaResult<T> assumeSuccessful() {
            assertThat(exception).as(firstRootCauseMessageLineOf(exception)).isNull();
            return this;
        }

        public JpaResult<T> assertSuccessful() {
            assertThat(exception).as(firstRootCauseMessageLineOf(exception)).isNull();
            return this;
        }

        public JpaResult<T> assertNotNull() {
            assertThat(returnedValue()).isNotNull();
            return this;
        }

        private String firstRootCauseMessageLineOf(final Throwable exception) {
            final var rootCause = NestedExceptionUtils.getRootCause(exception);
            return Optional.ofNullable(rootCause != null ? rootCause : exception)
                    .map(Throwable::getMessage)
                    .map(message -> message.split("\\r|\\n|\\r\\n", 0)[0])
                    .orElse(null);
        }
    }

}
