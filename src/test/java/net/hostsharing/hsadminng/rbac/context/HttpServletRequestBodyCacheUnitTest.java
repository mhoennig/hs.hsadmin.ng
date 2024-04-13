package net.hostsharing.hsadminng.rbac.context;

import net.hostsharing.hsadminng.context.HttpServletRequestBodyCache;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

class HttpServletRequestBodyCacheUnitTest {

    @Test
    void readsTheStream() {
        // given
        try (final var givenBodyCache = new HttpServletRequestBodyCache("Hallo".getBytes())) {

            // when
            final var actual = new String(givenBodyCache.readAllBytes());

            // then
            assertThat(actual).isEqualTo("Hallo");

        } catch (final IOException exc) {
            throw new AssertionError("unexpected IO exception", exc);
        }
    }

    @Test
    void isReadyReturnsTrue() {
        // given
        try (final var givenBodyCache = new HttpServletRequestBodyCache("Hallo".getBytes())) {

            // when
            final var actual = givenBodyCache.isReady();

            // then
            assertThat(actual).isTrue();

        } catch (final IOException exc) {
            throw new AssertionError("unexpected IO exception", exc);
        }
    }

    @Test
    void isFinishedReturnsTrueWhenNotRead() {
        // given
        try (final var givenBodyCache = new HttpServletRequestBodyCache("Hallo".getBytes())) {

            // when
            final var actual = givenBodyCache.isFinished();

            // then
            assertThat(actual).isFalse();

        } catch (final IOException exc) {
            throw new AssertionError("unexpected IO exception", exc);
        }
    }

    @Test
    void isFinishedReturnsTrueWhenRead() {
        // given
        try (final var givenBodyCache = new HttpServletRequestBodyCache("Hallo".getBytes())) {
            givenBodyCache.readAllBytes();

            // when
            final var actual = givenBodyCache.isFinished();

            // then
            assertThat(actual).isTrue();

        } catch (final IOException exc) {
            throw new AssertionError("unexpected IO exception", exc);
        }
    }

    @Test
    void isFinishedReturnsTrueOnException() {
        // given
        try (final var givenBodyCache = spy(new HttpServletRequestBodyCache("".getBytes()))) {
            given(givenBodyCache.available()).willThrow(new IOException("fake exception"));

            // when
            final var actual = givenBodyCache.isFinished();

            // then
            assertThat(actual).isTrue();

        } catch (final IOException exc) {
            throw new AssertionError("unexpected IO exception", exc);
        }
    }

    @Test
    void setReadListenerThrowsNotImplementedException() {
        // given
        try (final var givenBodyCache = new HttpServletRequestBodyCache("Hallo".getBytes())) {

            // when
            final var exception = assertThrows(RuntimeException.class, () -> givenBodyCache.setReadListener(null));

            // then
            assertThat(exception.getMessage()).isEqualTo("Not implemented");

        } catch (final IOException exc) {
            throw new AssertionError("unexpected IO exception", exc);
        }
    }
}
