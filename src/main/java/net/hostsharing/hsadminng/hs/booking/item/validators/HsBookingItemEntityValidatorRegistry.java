package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import net.hostsharing.hsadminng.errors.MultiValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.PRIVATE_CLOUD;

public class HsBookingItemEntityValidatorRegistry {

    private static final Map<Enum<HsBookingItemType>, HsEntityValidator<HsBookingItemEntity>> validators = new HashMap<>();
    static {
        register(PRIVATE_CLOUD, new HsPrivateCloudBookingItemValidator());
        register(CLOUD_SERVER, new HsCloudServerBookingItemValidator());
        register(MANAGED_SERVER, new HsManagedServerBookingItemValidator());
        register(MANAGED_WEBSPACE, new HsManagedWebspaceBookingItemValidator());
    }

    private static void register(final Enum<HsBookingItemType> type, final HsEntityValidator<HsBookingItemEntity> validator) {
        stream(validator.propertyValidators).forEach( entry -> {
            entry.verifyConsistency(Map.entry(type, validator));
        });
        validators.put(type, validator);
    }

    public static HsEntityValidator<HsBookingItemEntity> forType(final Enum<HsBookingItemType> type) {
        if ( validators.containsKey(type)) {
            return validators.get(type);
        }
        throw new IllegalArgumentException("no validator found for type " + type);
    }

    public static  Set<Enum<HsBookingItemType>> types() {
        return validators.keySet();
    }

    public static List<String> doValidate(final HsBookingItemEntity bookingItem) {
        return HsBookingItemEntityValidatorRegistry.forType(bookingItem.getType()).validate(bookingItem);
    }

    public static HsBookingItemEntity validated(final HsBookingItemEntity entityToSave) {
        MultiValidationException.throwInvalid(doValidate(entityToSave));
        return entityToSave;
    }
}
