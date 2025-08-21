package net.hostsharing.hsadminng.persistence;


import org.hibernate.Hibernate;

import java.util.UUID;

public interface ImmutableBaseEntity<T extends ImmutableBaseEntity<?>> {
    UUID getUuid();

    default T load() {
        Hibernate.initialize(this);
        //noinspection unchecked
        return (T) this;
    };
}
