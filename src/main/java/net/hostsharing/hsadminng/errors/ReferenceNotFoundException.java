package net.hostsharing.hsadminng.errors;

import java.util.UUID;

public class ReferenceNotFoundException extends RuntimeException {

    private final Class<?> entityClass;
    private final UUID uuid;
    public <E> ReferenceNotFoundException(final Class<E> entityClass, final UUID uuid, final Throwable exc) {
        super(exc);
        this.entityClass = entityClass;
        this.uuid = uuid;
    }

    @Override
    public String getMessage() {
        return "Cannot resolve " + entityClass.getSimpleName() +" with uuid " + uuid;
    }
}
