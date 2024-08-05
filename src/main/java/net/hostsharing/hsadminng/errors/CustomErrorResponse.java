package net.hostsharing.hsadminng.errors;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@Getter
public class CustomErrorResponse {

    static ResponseEntity<CustomErrorResponse> errorResponse(
            final WebRequest request,
            final HttpStatus httpStatus,
            final String message) {
        return new ResponseEntity<>(
                new CustomErrorResponse(request.getContextPath(), httpStatus, message), httpStatus);
    }

    static String firstMessageLine(final Throwable exception) {
        if (exception.getMessage() != null) {
            return line(exception.getMessage(), 0);
        }
        return "ERROR: [500] " + exception.getClass().getName();
    }

    static String line(final String message, final int lineNo) {
        return message.split("\\r|\\n|\\r\\n", 0)[lineNo];
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    private final LocalDateTime timestamp;

    private final String path;

    private final int statusCode;

    private final String statusPhrase;

    private final String message;

    CustomErrorResponse(final String path, final HttpStatus status, final String message) {
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.statusCode = status.value();
        this.statusPhrase = status.getReasonPhrase();
        this.message = message.startsWith("ERROR: [") ? message : "ERROR: [" + statusCode + "] " + message;
    }
}
