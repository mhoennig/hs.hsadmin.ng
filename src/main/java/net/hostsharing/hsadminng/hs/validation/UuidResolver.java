package net.hostsharing.hsadminng.hs.validation;

import lombok.experimental.UtilityClass;

import jakarta.validation.ValidationException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@UtilityClass
public class UuidResolver {

    public static <T> T resolve(final String jsonPath, final UUID uuid, final Function<UUID, Optional<T>> findByUuid) {
        return findByUuid.apply(uuid)
                .orElseThrow(() -> new ValidationException("Unable to find " + jsonPath + ": " + uuid));
    }
}
