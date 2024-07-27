package net.hostsharing.hsadminng.rbac.rbacobject;


import org.hibernate.Hibernate;

import java.util.UUID;

public interface RbacObject<T extends RbacObject<?>> {
    UUID getUuid();

    int getVersion();

    default T load() {
        Hibernate.initialize(this);
        //noinspection unchecked
        return (T) this;
    };
}
