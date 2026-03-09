package net.hostsharing.hsadminng.repr;

import lombok.val;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
class StringifyableUnitTest {

    @Test
    void toShortString_whenEntityImplementsStringifyable_usesItsToShortString() {
        // given
        val entity = new StringifyableTestEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "short-repr"
        );

        // when
        val result = Stringifyable.toShortString(entity);

        // then
        assertEquals("short-repr", result);
    }

    @Test
    void toShortString_whenEntityDoesNotImplementStringifyable_returnsUuidString() {
        // given
        val entity = new NonStringifyableTestEntity(UUID.fromString("00000000-0000-0000-0000-000000000002"));

        // when
        val result = Stringifyable.toShortString(entity);

        // then
        assertEquals("00000000-0000-0000-0000-000000000002", result);
    }

    private static final class NonStringifyableTestEntity implements BaseEntity<NonStringifyableTestEntity> {
        private final UUID uuid;

        private NonStringifyableTestEntity(final UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public int getVersion() {
            return 0;
        }
    }

    private static final class StringifyableTestEntity implements BaseEntity<StringifyableTestEntity>, Stringifyable {
        private final UUID uuid;
        private final String shortString;

        private StringifyableTestEntity(final UUID uuid, final String shortString) {
            this.uuid = uuid;
            this.shortString = shortString;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public String toShortString() {
            return shortString;
        }
    }
}
