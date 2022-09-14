package net.hostsharing.hsadminng.errors;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

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
