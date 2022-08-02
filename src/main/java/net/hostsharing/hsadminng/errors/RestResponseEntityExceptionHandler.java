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

@ControllerAdvice
public class RestResponseEntityExceptionHandler
    extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<CustomErrorResponse> handleConflict(
        final RuntimeException exc, final WebRequest request) {

        return new ResponseEntity<>(
            new CustomErrorResponse(exc, HttpStatus.CONFLICT), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(JpaSystemException.class)
    protected ResponseEntity<CustomErrorResponse> handleJpaExceptions(
        final RuntimeException exc, final WebRequest request) {

        return new ResponseEntity<>(
            new CustomErrorResponse(exc, HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
    }
}

@Getter
class CustomErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    private final LocalDateTime timestamp;

    private final HttpStatus status;

    private final String message;

    public CustomErrorResponse(final RuntimeException exc, final HttpStatus status) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = firstLine(NestedExceptionUtils.getMostSpecificCause(exc).getMessage());
    }

    private String firstLine(final String message) {
        return message.split("\\r|\\n|\\r\\n", 0)[0];
    }
}
