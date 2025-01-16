package net.hostsharing.hsadminng.persistence;


import org.hibernate.Hibernate;

import jakarta.persistence.EntityManager;
import java.util.UUID;

public interface BaseEntity<T extends BaseEntity<?>> {
    UUID getUuid();

    int getVersion();

    default T load() {
        Hibernate.initialize(this);
        //noinspection unchecked
        return (T) this;
    };

    default T reload(final EntityManager em) {
        em.flush();
        em.refresh(this);
        return load();
    }
}
