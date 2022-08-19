package net.hostsharing.hsadminng.errors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RestResponseEntityExceptionHandlerUnitTest {

    final RestResponseEntityExceptionHandler exceptionHandler = new RestResponseEntityExceptionHandler();

    @Test
    void handleConflict() {
        // given
        final var givenException = new DataIntegrityViolationException("First Line\nSecond Line\nThird Line");
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleConflict(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(409);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("First Line");
    }

    @Test
    void jpaExceptionWithKnownErrorCode() {
        // given
        final var givenException = new JpaSystemException(new RuntimeException(
                "ERROR: [401] First Line\nSecond Line\nThird Line"));
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleJpaExceptions(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(401);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [401] First Line");
    }

    @Test
    void jpaExceptionWithUnknownErrorCode() {
        // given
        final var givenException = new JpaSystemException(new RuntimeException(
                "ERROR: [999] First Line\nSecond Line\nThird Line"));
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleJpaExceptions(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(500);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [999] First Line");
    }

    @Test
    void handleOtherExceptionsWithoutErrorCode() {
        // given
        final var givenThrowable = new Error("First Line\nSecond Line\nThird Line");
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleOtherExceptions(givenThrowable, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(500);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("First Line");
    }

    @Test
    void handleOtherExceptionsWithErrorCode() {
        // given
        final var givenThrowable = new Error("ERROR: [418] First Line\nSecond Line\nThird Line");
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleOtherExceptions(givenThrowable, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(418);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [418] First Line");
    }

}
