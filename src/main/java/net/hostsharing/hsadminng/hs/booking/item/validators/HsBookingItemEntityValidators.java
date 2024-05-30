package net.hostsharing.hsadminng.hs.booking.item.validators;

import lombok.experimental.UtilityClass;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;

@UtilityClass
public class HsBookingItemEntityValidators {

    private static final Map<Enum<HsBookingItemType>, HsEntityValidator<HsBookingItemEntity, HsBookingItemType>> validators = new HashMap<>();
    static {
        register(CLOUD_SERVER, new HsCloudServerBookingItemValidator());
        register(MANAGED_SERVER, new HsManagedServerBookingItemValidator());
        register(MANAGED_WEBSPACE, new HsManagedWebspaceBookingItemValidator());
    }

    private static void register(final Enum<HsBookingItemType> type, final HsEntityValidator<HsBookingItemEntity, HsBookingItemType> validator) {
        stream(validator.propertyValidators).forEach( entry -> {
            entry.verifyConsistency(Map.entry(type, validator));
        });
        validators.put(type, validator);
    }

    public static HsEntityValidator<HsBookingItemEntity, HsBookingItemType> forType(final Enum<HsBookingItemType> type) {
        return validators.get(type);
    }

    public static  Set<Enum<HsBookingItemType>> types() {
        return validators.keySet();
    }

    public static HsBookingItemEntity valid(final HsBookingItemEntity entityToSave) {
        final var violations = HsBookingItemEntityValidators.forType(entityToSave.getType()).validate(entityToSave);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.toString());
        }
        return entityToSave;
    }
}
