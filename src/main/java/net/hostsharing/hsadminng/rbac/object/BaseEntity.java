package net.hostsharing.hsadminng.rbac.object;


import org.hibernate.Hibernate;

import java.util.UUID;

// TODO.impl: this class does not really belong into this package, but there is no right place yet
public interface BaseEntity<T extends BaseEntity<?>> {
    UUID getUuid();

    int getVersion();

    default T load() {
        Hibernate.initialize(this);
        //noinspection unchecked
        return (T) this;
    };
}
