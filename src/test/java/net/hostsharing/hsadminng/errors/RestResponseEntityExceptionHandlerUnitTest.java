package net.hostsharing.hsadminng.errors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(409);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [409] First Line");
    }

    @Test
    void handleForeignKeyViolation() {
        // given
        final var givenException = new DataIntegrityViolationException("""
            ... violates foreign key constraint ...
               Detail:   Second Line
            Third Line
            """);
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleConflict(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(400);
        assertThat(errorResponse.getBody()).isNotNull()
                .extracting(CustomErrorResponse::getMessage).isEqualTo("ERROR: [400] Second Line");
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
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(401);
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
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [400] Unable to find Partner with uuid 12345-123454");
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
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo(
                "ERROR: [400] Unable to find net.hostsharing.hsadminng.WhateverEntity with id 12345-123454");
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
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [400] whatever error message");
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
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [400] Unable to find NoDisplayNameEntity with uuid 12345-123454");
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
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(500);
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
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(404);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [404] some error message");
    }

    @ParameterizedTest
    @ValueSource(classes = {
            org.iban4j.InvalidCheckDigitException.class,
            org.iban4j.IbanFormatException.class,
            org.iban4j.BicFormatException.class })
    void handlesIbanAndBicExceptions(final Class<? extends RuntimeException> givenExceptionClass)
            throws Exception {
        // given
        final var givenException = givenExceptionClass.getConstructor(String.class).newInstance("given error message");
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleValidationExceptions(givenException, givenWebRequest);

        // then
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(400);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [400] given error message");
    }

    @Test
    void handleMethodArgumentNotValidException() {
        // given
        final var givenBindingResult = mock(BindingResult.class);
        when(givenBindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("someObject", "someField", "someRejectedValue", false, null, null, "expected to be something")
        ));
        final var givenException = new MethodArgumentNotValidException(
                mock(MethodParameter.class),
                givenBindingResult

        );
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleMethodArgumentNotValid(givenException,
                HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, givenWebRequest);

        // then
        assertThat(errorResponse.getBody())
                .isInstanceOf(CustomErrorResponse.class)
                .extracting("statusCode").isEqualTo(400);
        assertThat(errorResponse.getBody())
                .extracting("message")
                // FYI: the brackets around the message are here because it's actually an array, in this case of size 1
                .isEqualTo("ERROR: [400] [someField expected to be something but is \"someRejectedValue\"]");
    }

    @Test
    void handleOtherExceptionsWithoutErrorCode() {
        // given
        final var givenThrowable = new Error("First Line\nSecond Line\nThird Line");
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleOtherExceptions(givenThrowable, givenWebRequest);

        // then
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(500);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [500] First Line");
    }

    @Test
    void handleOtherExceptionsWithErrorCode() {
        // given
        final var givenThrowable = new Error("ERROR: [418] First Line\nSecond Line\nThird Line");
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleOtherExceptions(givenThrowable, givenWebRequest);

        // then
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(418);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [418] First Line");
    }

    @Test
    void handleOtherExceptionsWithoutMessage() {
        // given
        final var givenThrowable = new Error();
        final var givenWebRequest = mock(WebRequest.class);

        // when
        final var errorResponse = exceptionHandler.handleOtherExceptions(givenThrowable, givenWebRequest);

        // then
        assertThat(errorResponse.getBody().getStatusCode()).isEqualTo(500);
        assertThat(errorResponse.getBody().getMessage()).isEqualTo("ERROR: [500] java.lang.Error");
    }

    public static class NoDisplayNameEntity {

    }

}
