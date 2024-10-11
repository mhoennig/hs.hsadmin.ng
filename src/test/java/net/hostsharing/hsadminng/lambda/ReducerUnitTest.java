package net.hostsharing.hsadminng.lambda;


import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

class ReducerUnitTest {

    @Test
    void throwsExceptionForMoreThanASingleElement() {
        final var givenStream = Stream.of(1, 2);

        final var exception = catchThrowable(() -> {
                //noinspection ResultOfMethodCallIgnored
                givenStream.reduce(Reducer::toSingleElement);
            }
        );

        assertThat(exception).isInstanceOf(AssertionError.class);
    }

    @Test
    void passesASingleElement() {
        final var givenStream = Stream.of(7);
        final var singleElement = givenStream.reduce(Reducer::toSingleElement);
        assertThat(singleElement).contains(7);
    }
}
