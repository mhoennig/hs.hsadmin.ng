package net.hostsharing.hsadminng.hs.office.test;

import net.hostsharing.hsadminng.persistence.HasUuid;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityList {

    public static <E extends HasUuid> E one(final List<E> entities) {
        assertThat(entities).hasSize(1);
        return entities.stream().findFirst().orElseThrow();
    }
}
