package net.hostsharing.hsadminng.persistence;



import jakarta.persistence.EntityManager;

public interface BaseEntity<T extends BaseEntity<?>> extends ImmutableBaseEntity<T> {

    int getVersion();

    default T reload(final EntityManager em) {
        em.flush();
        em.refresh(this);
        return load();
    }
}
