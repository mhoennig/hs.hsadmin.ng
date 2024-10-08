package net.hostsharing.hsadminng.persistence;


import org.hibernate.Hibernate;

import java.util.UUID;

public interface BaseEntity<T extends BaseEntity<?>> {
    UUID getUuid();

    int getVersion();

    default T load() {
        Hibernate.initialize(this);
        //noinspection unchecked
        return (T) this;
    };
}
