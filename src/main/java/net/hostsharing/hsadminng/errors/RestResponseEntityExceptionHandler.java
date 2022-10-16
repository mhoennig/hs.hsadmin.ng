package net.hostsharing.hsadminng.errors;

import org.iban4j.Iban4jException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.persistence.EntityNotFoundException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.errors.CustomErrorResponse.*;

@ControllerAdvice
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<CustomErrorResponse> handleConflict(
            final RuntimeException exc, final WebRequest request) {

        final var message = firstLine(NestedExceptionUtils.getMostSpecificCause(exc).getMessage());
        return errorResponse(request, HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(JpaSystemException.class)
    protected ResponseEntity<CustomErrorResponse> handleJpaExceptions(
            final RuntimeException exc, final WebRequest request) {
        final var message = firstLine(NestedExceptionUtils.getMostSpecificCause(exc).getMessage());
        return errorResponse(request, httpStatus(message).orElse(HttpStatus.INTERNAL_SERVER_ERROR), message);
    }

    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<CustomErrorResponse> handleNoSuchElementException(
            final RuntimeException exc, final WebRequest request) {
        final var message = firstLine(NestedExceptionUtils.getMostSpecificCause(exc).getMessage());
        return errorResponse(request, HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler({ JpaObjectRetrievalFailureException.class, EntityNotFoundException.class })
    protected ResponseEntity<CustomErrorResponse> handleJpaObjectRetrievalFailureException(
            final RuntimeException exc, final WebRequest request) {
        final var message =
                userReadableEntityClassName(
                        firstLine(NestedExceptionUtils.getMostSpecificCause(exc).getMessage()));
        return errorResponse(request, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Iban4jException.class)
    protected ResponseEntity<CustomErrorResponse> handleIbanAndBicExceptions(
            final Throwable exc, final WebRequest request) {
        final var message = firstLine(NestedExceptionUtils.getMostSpecificCause(exc).getMessage());
        return errorResponse(request, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Throwable.class)
    protected ResponseEntity<CustomErrorResponse> handleOtherExceptions(
            final Throwable exc, final WebRequest request) {
        final var message = firstMessageLine(NestedExceptionUtils.getMostSpecificCause(exc));
        return errorResponse(request, httpStatus(message).orElse(HttpStatus.INTERNAL_SERVER_ERROR), message);
    }

    @Override
    @SuppressWarnings("unchecked,rawtypes")
    protected ResponseEntity handleExceptionInternal(
            Exception exc, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

        final var response = super.handleExceptionInternal(exc, body, headers, status, request);
        return errorResponse(request, response.getStatusCode(),
                Optional.ofNullable(response.getBody()).map(Object::toString).orElse(firstMessageLine(exc)));
    }

    //@ExceptionHandler({ MethodArgumentNotValidException.class })
    @SuppressWarnings("unchecked,rawtypes")
    protected ResponseEntity handleMethodArgumentNotValid(
            MethodArgumentNotValidException exc,
            HttpHeaders headers,
            HttpStatus status,
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

    private String userReadableEntityClassName(final String exceptionMessage) {
        final var regex = "(net.hostsharing.hsadminng.[a-z0-9_.]*.[A-Za-z0-9_$]*Entity) ";
        final var pattern = Pattern.compile(regex);
        final var matcher = pattern.matcher(exceptionMessage);
        if (matcher.find()) {
            final var entityName = matcher.group(1);
            final var entityClass = resolveClass(entityName);
            if (entityClass.isPresent()) {
                return (entityClass.get().isAnnotationPresent(DisplayName.class)
                        ? exceptionMessage.replace(entityName, entityClass.get().getAnnotation(DisplayName.class).value())
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

    private Optional<HttpStatus> httpStatus(final String message) {
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
