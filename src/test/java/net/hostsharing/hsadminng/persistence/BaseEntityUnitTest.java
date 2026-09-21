package net.hostsharing.hsadminng.persistence;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BaseEntityUnitTest {

    static class TestBaseEntity implements BaseEntity<TestBaseEntity> {
        private final UUID uuid = UUID.randomUUID();
        @Override
        public UUID getUuid() {
            return uuid;
        }
        @Override
        public int getVersion() {
            return 0;
        }
    }

    @Test
    void reloadFlushesRefreshesAndReturnsSameInstance() {
        final var entity = new TestBaseEntity();
        final var em = mock(EntityManager.class);
        try (MockedStatic<Hibernate> mocked = mockStatic(Hibernate.class)) {
            final var result = entity.reload(em);
            verify(em).flush();
            verify(em).refresh(entity);
            mocked.verify(() -> Hibernate.initialize(entity));
            assertThat(result).isSameAs(entity).isNotNull();
        }
    }

    @Test
    void reloadReturnsSameInstance() {
        final var entity = new TestBaseEntity();
        final var em = mock(EntityManager.class);
        try (MockedStatic<Hibernate> mocked = mockStatic(Hibernate.class)) {
            assertThat(entity.reload(em)).isSameAs(entity);
        }
    }
}
