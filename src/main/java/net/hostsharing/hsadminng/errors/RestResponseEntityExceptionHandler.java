package net.hostsharing.hsadminng.errors;

import org.iban4j.Iban4jException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaSystemException;
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
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.errors.CustomErrorResponse.*;

@ControllerAdvice
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<CustomErrorResponse> handleConflict(
            final RuntimeException exc, final WebRequest request) {

        final var rawMessage = NestedExceptionUtils.getMostSpecificCause(exc).getMessage();
        var message = line(rawMessage, 0);
        if (message.contains("violates foreign key constraint")) {
            return errorResponse(request, HttpStatus.BAD_REQUEST, line(rawMessage, 1).replaceAll(" *Detail: *", ""));
        }
        return errorResponse(request, HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(JpaSystemException.class)
    protected ResponseEntity<CustomErrorResponse> handleJpaExceptions(
            final RuntimeException exc, final WebRequest request) {
        final var message = line(NestedExceptionUtils.getMostSpecificCause(exc).getMessage(), 0);
        return errorResponse(request, httpStatus(exc, message).orElse(HttpStatus.INTERNAL_SERVER_ERROR), message);
    }

    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<CustomErrorResponse> handleNoSuchElementException(
            final RuntimeException exc, final WebRequest request) {
        final var message = line(NestedExceptionUtils.getMostSpecificCause(exc).getMessage(), 0);
        return errorResponse(request, HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(ReferenceNotFoundException.class)
    protected ResponseEntity<CustomErrorResponse> handleReferenceNotFoundException(
            final ReferenceNotFoundException exc, final WebRequest request) {
        return errorResponse(request, HttpStatus.BAD_REQUEST, exc.getMessage());
    }

    @ExceptionHandler({ JpaObjectRetrievalFailureException.class, EntityNotFoundException.class })
    protected ResponseEntity<CustomErrorResponse> handleJpaObjectRetrievalFailureException(
            final RuntimeException exc, final WebRequest request) {
        final var message =
                userReadableEntityClassName(
                        line(NestedExceptionUtils.getMostSpecificCause(exc).getMessage(), 0));
        return errorResponse(request, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({ Iban4jException.class, ValidationException.class })
    protected ResponseEntity<CustomErrorResponse> handleValidationExceptions(
            final Throwable exc, final WebRequest request) {
        final String fullMessage = NestedExceptionUtils.getMostSpecificCause(exc).getMessage();
        final var message = exc instanceof MultiValidationException ? fullMessage : line(fullMessage, 0);
        return errorResponse(request, HttpStatus.BAD_REQUEST, message);
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
                Optional.ofNullable(response.getBody()).map(Object::toString).orElse(firstMessageLine(exc)));
    }
    @Override
    @SuppressWarnings("unchecked,rawtypes")
    protected ResponseEntity handleHttpMessageNotReadable(
            HttpMessageNotReadableException exc, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        final var message = line(exc.getMessage(), 0);
        return errorResponse(request, HttpStatus.BAD_REQUEST, message);
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
                .getAllValidationResults()
                .stream()
                .map(ParameterValidationResult::getResolvableErrors)
                .flatMap(Collection::stream)
                .filter(FieldError.class::isInstance)
                .map(FieldError.class::cast)
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage() + " but is \""
                        + fieldError.getRejectedValue() + "\"")
                .toList();
        return errorResponse(request, HttpStatus.BAD_REQUEST, errorList.toString());
    }


    private String userReadableEntityClassName(final String exceptionMessage) {
        final var regex = "(net.hostsharing.hsadminng.[a-z0-9_.]*.[A-Za-z0-9_$]*Entity) ";
        final var pattern = Pattern.compile(regex);
        final var matcher = pattern.matcher(exceptionMessage);
        if (matcher.find()) {
            final var entityName = matcher.group(1);
            final var entityClass = resolveClass(entityName);
            if (entityClass.isPresent()) {
                return (entityClass.get().isAnnotationPresent(DisplayAs.class)
                        ? exceptionMessage.replace(entityName, entityClass.get().getAnnotation(DisplayAs.class).value())
                        : exceptionMessage.replace(entityName, entityClass.get().getSimpleName()))
                        .replace(" with id ", " with uuid ");
            }

        }
        return exceptionMessage;
    }

    private static Optional<Class<?>> resolveClass(final String entityName) {
        try {
            return Optional.of(ClassLoader.getSystemClassLoader().loadClass(entityName));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
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

}
