package net.hostsharing.hsadminng.repr;

import net.hostsharing.hsadminng.persistence.BaseEntity;

public interface Stringifyable {

    static String toShortString(final BaseEntity<?> entity) {
        return entity instanceof Stringifyable stringifyableEntity
                ? stringifyableEntity.toShortString()
                : entity.getUuid().toString();
    }

    String toShortString();
}
