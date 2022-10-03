package net.hostsharing.hsadminng.errors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.context.request.WebRequest;

import javax.persistence.EntityNotFoundException;

import java.util.NoSuchElementException;

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
    void handleJpaObjectRetrievalFailureExceptionWithDisplayName() {
        // given
        final var givenException = new JpaObjectRetrievalFailureException(
                new EntityNotFoundException(
                        "Unable to find net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity with id 12345-123454")
        );
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleJpaObjectRetrievalFailureException(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("Unable to find Partner with uuid 12345-123454");
    }

    @Test
    void handleJpaObjectRetrievalFailureExceptionIfEntityClassCannotBeDetermined() {
        // given
        final var givenException = new JpaObjectRetrievalFailureException(
                new EntityNotFoundException(
                        "Unable to find net.hostsharing.hsadminng.WhateverEntity with id 12345-123454")
        );
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleJpaObjectRetrievalFailureException(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo(
                "Unable to find net.hostsharing.hsadminng.WhateverEntity with id 12345-123454");
    }

    @Test
    void handleJpaObjectRetrievalFailureExceptionIfPatternDoesNotMatch() {
        // given
        final var givenException = new JpaObjectRetrievalFailureException(
                new EntityNotFoundException("whatever error message")
        );
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleJpaObjectRetrievalFailureException(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("whatever error message");
    }

    @Test
    void handleJpaObjectRetrievalFailureExceptionWithEntityName() {
        // given
        final var givenException = new JpaObjectRetrievalFailureException(
                new EntityNotFoundException("Unable to find "
                        + NoDisplayNameEntity.class.getTypeName()
                        + " with id 12345-123454")
        );
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleJpaObjectRetrievalFailureException(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("Unable to find NoDisplayNameEntity with uuid 12345-123454");
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
    void handleNoSuchElementException() {
        // given
        final var givenException = new NoSuchElementException("some error message");
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleNoSuchElementException(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(404);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("some error message");
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

    public static class NoDisplayNameEntity {

    }

}
