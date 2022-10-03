package net.hostsharing.hsadminng.errors;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

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

    private String userReadableEntityClassName(final String exceptionMessage) {
        final var regex = "(net.hostsharing.hsadminng.[a-z0-9_.]*.[A-Za-z0-9_$]*Entity) ";
        final var pattern = Pattern.compile(regex);
        final var matcher = pattern.matcher(exceptionMessage);
        if (matcher.find()) {
            final var entityName = matcher.group(1);
            final var entityClass = resolveClassOrNull(entityName);
            if (entityClass != null ) {
                return (entityClass.isAnnotationPresent(DisplayName.class)
                        ? exceptionMessage.replace(entityName, entityClass.getAnnotation(DisplayName.class).value())
                        : exceptionMessage.replace(entityName, entityClass.getSimpleName()))
                        .replace(" with id ", " with uuid ");
            }

        }
        return exceptionMessage;
    }

    private static Class<?> resolveClassOrNull(final String entityName) {
        try {
            return ClassLoader.getSystemClassLoader().loadClass(entityName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @ExceptionHandler(Throwable.class)
    protected ResponseEntity<CustomErrorResponse> handleOtherExceptions(
            final Throwable exc, final WebRequest request) {
        final var message = firstLine(NestedExceptionUtils.getMostSpecificCause(exc).getMessage());
        return errorResponse(request, httpStatus(message).orElse(HttpStatus.INTERNAL_SERVER_ERROR), message);
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

    private static ResponseEntity<CustomErrorResponse> errorResponse(
            final WebRequest request,
            final HttpStatus httpStatus,
            final String message) {
        return new ResponseEntity<>(
                new CustomErrorResponse(request.getContextPath(), httpStatus, message), httpStatus);
    }

    private String firstLine(final String message) {
        return message.split("\\r|\\n|\\r\\n", 0)[0];
    }
}

@Getter
class CustomErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    private final LocalDateTime timestamp;

    private final String path;

    private final int status;

    private final String error;

    private final String message;

    public CustomErrorResponse(final String path, final HttpStatus status, final String message) {
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
    }
}
