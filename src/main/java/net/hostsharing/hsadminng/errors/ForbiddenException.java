package net.hostsharing.hsadminng.errors;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(final String message) {
        super(message);
    }
}
