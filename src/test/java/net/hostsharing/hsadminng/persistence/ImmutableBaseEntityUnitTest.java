package net.hostsharing.hsadminng.persistence;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class ImmutableBaseEntityUnitTest {

    static class TestEntity implements ImmutableBaseEntity<TestEntity> {
        private final UUID uuid = UUID.randomUUID();
        @Override
        public UUID getUuid() {
            return uuid;
        }
    }

    @Test
    void loadReturnsSameInstanceAndInitializes() {
        final var entity = new TestEntity();
        try (MockedStatic<Hibernate> mocked = mockStatic(Hibernate.class)) {
            final var result = entity.load();
            mocked.verify(() -> Hibernate.initialize(entity));
            assertThat(result).isSameAs(entity).isNotNull();
        }
    }

    @Test
    void loadReturnsNotNull() {
        final var entity = new TestEntity();
        try (MockedStatic<Hibernate> mocked = mockStatic(Hibernate.class)) {
            assertThat(entity.load()).isNotNull().isSameAs(entity);
        }
    }
}
