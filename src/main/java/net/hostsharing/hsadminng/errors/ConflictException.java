package net.hostsharing.hsadminng.errors;

public class ConflictException extends RuntimeException {

    public ConflictException(final String message) {
        super(message);
    }
}
