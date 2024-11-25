package net.hostsharing.hsadminng.test;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

@UtilityClass
public class TestUuidGenerator {

    private static final UUID ZEROES_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final List<UUID> GIVEN_UUIDS = List.of(
            ZEROES_UUID,
            uuidWithDigit(1),
            uuidWithDigit(2),
            uuidWithDigit(3),
            uuidWithDigit(4),
            uuidWithDigit(5),
            uuidWithDigit(6),
            uuidWithDigit(7),
            uuidWithDigit(8),
            uuidWithDigit(9),
            uuidWithChar('a'),
            uuidWithChar('b'),
            uuidWithChar('c'),
            uuidWithChar('d'),
            uuidWithChar('e'),
            uuidWithChar('f')
            );

    private static Set<Integer> staticallyUsedIndexes = new HashSet<>();

    private Queue<UUID> availableUuids = null;


    public static void start(final int firstIndex) {
        if (staticallyUsedIndexes.contains(firstIndex)) {
            throw new IllegalArgumentException(firstIndex + " already used statically, try higher and amend references");
        }
        availableUuids = new LinkedList<>(GIVEN_UUIDS.subList(firstIndex, GIVEN_UUIDS.size()));
    }

    public static UUID next() {
        if (availableUuids == null) {
            throw new IllegalStateException("UUID generator not started yet, call start() in @BeforeEach.");
        }
        if (availableUuids.isEmpty()) {
            throw new IllegalStateException("No UUIDs available anymore.");
        }
        return availableUuids.poll();
    }

    /**
     * Marks the UUID as used in static initializers.
     *
     * @param index 0..15
     * @return a constant UUID related to the given index
     */
    public static UUID use(final int index) {
        staticallyUsedIndexes.add(index);
        return GIVEN_UUIDS.get(index);
    }

    /**
     * References the UUID from the given index.
     *
     * @param index 0..15
     * @return a constant UUID related to the given index
     */
    public static UUID ref(final int index) {
        return GIVEN_UUIDS.get(index);
    }

    private static @NotNull UUID uuidWithDigit(final int digit) {
        return UUID.fromString(ZEROES_UUID.toString().replace('0', Character.forDigit(digit, 16)));
    }

    private static @NotNull UUID uuidWithChar(final char hexDigit) {
        return UUID.fromString(ZEROES_UUID.toString().replace('0', hexDigit));
    }
}
