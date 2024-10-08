package net.hostsharing.hsadminng.rbac.test;

import net.hostsharing.hsadminng.persistence.BaseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityList {

    public static <E extends BaseEntity> E one(final List<E> entities) {
        assertThat(entities).hasSize(1);
        return entities.stream().findFirst().orElseThrow();
    }
}
