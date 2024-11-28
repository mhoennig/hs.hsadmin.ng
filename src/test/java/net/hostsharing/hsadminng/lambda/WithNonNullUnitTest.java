package net.hostsharing.hsadminng.lambda;

import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.lambda.WithNonNull.withNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class WithNonNullUnitTest {

    Boolean didRun = null;

    @Test
    void withNonNullRunsBodyIfNotNull() {
        didRun = false;
        withNonNull("test", nonNullValue -> {
            assertThat(nonNullValue).isEqualTo("test");
            didRun = true;
        } );
        assertThat(didRun).isTrue();
    }

    @Test
    void withNonNullDoesNotRunBodyIfNull() {
        didRun = false;
        withNonNull(null, nonNullValue -> {
            didRun = true;
        } );
        assertThat(didRun).isFalse();
    }
}
