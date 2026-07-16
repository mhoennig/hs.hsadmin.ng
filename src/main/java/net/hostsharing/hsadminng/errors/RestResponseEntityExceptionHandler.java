package net.hostsharing.hsadminng.errors;

import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.RequiredArgsConstructor;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.RetroactiveTranslator;
import org.iban4j.Iban4jException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import java.util.*;
import java.util.function.Function;

import static net.hostsharing.hsadminng.errors.CustomErrorResponse.*;

@ControllerAdvice
@RequiredArgsConstructor
// HOWTO error handler mapping exceptions to specific http error codes and sensible error messages
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    @Autowired
    private final MessageTranslator messageTranslator;

    @Autowired(required = false)
    private final List<RetroactiveTranslator> retroactiveTranslators;

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<CustomErrorResponse> handleAccessDeniedException(
            final AccessDeniedException exc, final WebRequest request) {
        final var fullMaybeLocalizedMessage = localizedMessage(NestedExceptionUtils.getMostSpecificCause(exc));
        final var sprippedMaybeLocalizedMessage = stripTechnicalDetails(fullMaybeLocalizedMessage);
        return errorResponse(request, HttpStatus.UNAUTHORIZED, sprippedMaybeLocalizedMessage);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<CustomErrorResponse> handleConflict(
            final RuntimeException exc, final WebRequest request) {

        final var causingException = NestedExceptionUtils.getMostSpecificCause(exc);
        final var fullMaybeLocalizedMessage = localizedMessage(causingException);
        final var maybeTranslatedMessage = tryTranslation(fullMaybeLocalizedMessage);
        final var strippedMaybeLocalizedMessage = stripTechnicalDetails(maybeTranslatedMessage);
        return errorResponse(
                request,
                // Some DB constraints raise client errors such as ERROR: [400],
                // those do not count as conflicts at the HTTP level.
                httpStatus(causingException, strippedMaybeLocalizedMessage).orElse(HttpStatus.CONFLICT),
                strippedMaybeLocalizedMessage);
    }

    @ExceptionHandler(JpaSystemException.class)
    protected ResponseEntity<CustomErrorResponse> handleJpaExceptions(
            final RuntimeException exc, final WebRequest request) {
        final var fullMaybeLocalizedMessage = localizedMessage(NestedExceptionUtils.getMostSpecificCause(exc));
        final var sprippedMaybeLocalizedMessage = stripTechnicalDetails(fullMaybeLocalizedMessage);
        return errorResponse(request, httpStatus(exc, sprippedMaybeLocalizedMessage).orElse(HttpStatus.INTERNAL_SERVER_ERROR), sprippedMaybeLocalizedMessage);
    }

    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<CustomErrorResponse> handleNoSuchElementException(
            final RuntimeException exc, final WebRequest request) {
        final var fullMaybeLocalizedMessage = localizedMessage(NestedExceptionUtils.getMostSpecificCause(exc));
        final var sprippedMaybeLocalizedMessage = stripTechnicalDetails(fullMaybeLocalizedMessage);
        return errorResponse(request, HttpStatus.NOT_FOUND, sprippedMaybeLocalizedMessage);
    }

    @ExceptionHandler(ReferenceNotFoundException.class)
    protected ResponseEntity<CustomErrorResponse> handleReferenceNotFoundException(
            final ReferenceNotFoundException exc, final WebRequest request) {
        return errorResponse(request, HttpStatus.BAD_REQUEST, localizedMessage(exc));
    }

    @ExceptionHandler(ForbiddenException.class)
    protected ResponseEntity<CustomErrorResponse> handleForbiddenException(
            final ForbiddenException exc, final WebRequest request) {
        return errorResponse(request, HttpStatus.FORBIDDEN, localizedMessage(exc));
    }

    @ExceptionHandler(ConflictException.class)
    protected ResponseEntity<CustomErrorResponse> handleConflictException(
            final ConflictException exc, final WebRequest request) {
        return errorResponse(request, HttpStatus.CONFLICT, localizedMessage(exc));
    }

    @ExceptionHandler({ JpaObjectRetrievalFailureException.class, EntityNotFoundException.class })
    protected ResponseEntity<CustomErrorResponse> handleJpaObjectRetrievalFailureException(
            final RuntimeException exc, final WebRequest request) {
        final var localizedMessage = localizedMessage(NestedExceptionUtils.getMostSpecificCause(exc));
        final var sprippedMaybeLocalizedMessage = stripTechnicalDetails(localizedMessage);
        return errorResponse(request, HttpStatus.BAD_REQUEST, sprippedMaybeLocalizedMessage);
    }

    @ExceptionHandler({ Iban4jException.class, ValidationException.class })
    protected ResponseEntity<CustomErrorResponse> handleValidationExceptions(
            final Throwable exc, final WebRequest request) {
        final var localizedMessage = localizedMessage(NestedExceptionUtils.getMostSpecificCause(exc));
        final var sprippedMaybeLocalizedMessage = exc instanceof MultiValidationException ? localizedMessage : stripTechnicalDetails(localizedMessage);
        return errorResponse(request, HttpStatus.BAD_REQUEST, sprippedMaybeLocalizedMessage);
    }

    @ExceptionHandler(Throwable.class)
    protected ResponseEntity<CustomErrorResponse> handleOtherExceptions(
            final Throwable exc, final WebRequest request) {
        final var causingException = NestedExceptionUtils.getMostSpecificCause(exc);
        final var message = firstMessageLine(causingException);
        return errorResponse(request, httpStatus(causingException, message).orElse(HttpStatus.INTERNAL_SERVER_ERROR), message);
    }

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    protected ResponseEntity handleExceptionInternal(
            Exception exc, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        final var response = super.handleExceptionInternal(exc, body, headers, statusCode, request);
        return errorResponse(request, HttpStatus.valueOf(statusCode.value()),
                Optional.ofNullable(response).map(HttpEntity::getBody).map(Object::toString).orElse(firstMessageLine(exc)));
    }

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    protected ResponseEntity handleHttpMessageNotReadable(
            HttpMessageNotReadableException exc, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        final var localizedMessage = localizedMessage(exc);
        final var strippedMaybeLocalizedMessage = stripTechnicalDetails(localizedMessage);
        final var translatedMessage = tryTranslation(strippedMaybeLocalizedMessage);
        final var messageWithProperty = prependRejectedProperty(
                NestedExceptionUtils.getMostSpecificCause(exc), translatedMessage);
        return customErrorResponse(request, HttpStatus.BAD_REQUEST, messageWithProperty);
    }

    private String prependRejectedProperty(final Throwable causingException, final String message) {
        final var propertyPath = jsonPropertyPath(causingException);
        if (propertyPath == null) {
            return message;
        }
        return messageTranslator.translate("general.property-{0}", propertyPath) + ": " + message;
    }

    private static String jsonPropertyPath(final Throwable causingException) {
        if (!(causingException instanceof JsonMappingException jsonMappingException)
                || jsonMappingException.getPath().isEmpty()) {
            return null;
        }
        final var propertyPath = new StringBuilder();
        for (final var reference : jsonMappingException.getPath()) {
            if (reference.getFieldName() != null) {
                if (propertyPath.length() > 0) {
                    propertyPath.append('.');
                }
                propertyPath.append(reference.getFieldName());
            } else {
                propertyPath.append('[').append(reference.getIndex()).append(']');
            }
        }
        return propertyPath.toString();
    }

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    protected ResponseEntity handleMethodArgumentNotValid(
            MethodArgumentNotValidException exc,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        final var errorList = exc
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage() + " but is \""
                        + fieldError.getRejectedValue() + "\"")
                .toList();
        return errorResponse(request, HttpStatus.BAD_REQUEST, errorList.toString());
    }

    @SuppressWarnings("unchecked,rawtypes")

    @Override
    protected ResponseEntity handleHandlerMethodValidationException(
            final HandlerMethodValidationException exc,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {
        final var errorList = exc
                .getParameterValidationResults()
                .stream()
                .map(ParameterValidationResult::getResolvableErrors)
                .flatMap(Collection::stream)
                .filter(FieldError.class::isInstance)
                .map(FieldError.class::cast)
                .map(toEnrichedFieldErrorMessage())
                .toList();
        return errorResponse(request, HttpStatus.BAD_REQUEST, errorList.toString());
    }

    private Function<FieldError, String> toEnrichedFieldErrorMessage() {
        final var translatedButIsLiteral = messageTranslator.translate("general.but-is");
        // TODO.i18n: the following does not work in all languages, e.g. not in right-to-left languages
        return fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage() +
                " " + translatedButIsLiteral + " " + optionallyQuoted(fieldError.getRejectedValue());
    }

    private String optionallyQuoted(final Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return "\"" + value + "\"";
    }

    private Optional<HttpStatus> httpStatus(final Throwable causingException, final String message) {
        if ( EntityNotFoundException.class.isInstance(causingException) ) {
            return Optional.of(HttpStatus.BAD_REQUEST);
        }
        if (message.startsWith("ERROR: [")) {
            for (HttpStatus status : HttpStatus.values()) {
                if (message.startsWith("ERROR: [" + status.value() + "]")) {
                    return Optional.of(status);
                }
            }
            return Optional.of(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return Optional.empty();
    }

    private String localizedMessage(final Throwable throwable) {
        // most libraries seem to provide the localized message in both properties, but just for the case:
        return throwable.getLocalizedMessage() != null ? throwable.getLocalizedMessage() : throwable.getMessage();
    }

    private String tryTranslation(final String message) {

        for ( RetroactiveTranslator rtx: retroactiveTranslators ) {
            if (rtx.canTranslate(message)) {
                return rtx.translate(message);
            }
        }
        return message;
    }

    private ResponseEntity<CustomErrorResponse> errorResponse(
            final WebRequest request,
            final HttpStatus httpStatus,
            final String maybeTranslatedMessage) {
        return customErrorResponse(request, httpStatus, tryTranslation(maybeTranslatedMessage));
    }
}
