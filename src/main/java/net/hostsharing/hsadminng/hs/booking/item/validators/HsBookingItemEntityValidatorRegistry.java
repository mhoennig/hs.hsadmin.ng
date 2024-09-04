package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItem;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import net.hostsharing.hsadminng.errors.MultiValidationException;

import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.PRIVATE_CLOUD;

public class HsBookingItemEntityValidatorRegistry {

    private static final Map<Enum<HsBookingItemType>, HsEntityValidator<HsBookingItem>> validators = new HashMap<>();
    static {
        register(PRIVATE_CLOUD, new HsPrivateCloudBookingItemValidator());
        register(CLOUD_SERVER, new HsCloudServerBookingItemValidator());
        register(MANAGED_SERVER, new HsManagedServerBookingItemValidator());
        register(MANAGED_WEBSPACE, new HsManagedWebspaceBookingItemValidator());
        register(DOMAIN_SETUP, new HsDomainSetupBookingItemValidator());
    }

    private static void register(final Enum<HsBookingItemType> type, final HsEntityValidator<HsBookingItem> validator) {
        stream(validator.propertyValidators).forEach( entry -> {
            entry.verifyConsistency(Map.entry(type, validator));
        });
        validators.put(type, validator);
    }

    public static HsEntityValidator<HsBookingItem> forType(final Enum<HsBookingItemType> type) {
        if ( validators.containsKey(type)) {
            return validators.get(type);
        }
        throw new IllegalArgumentException("no validator found for type " + type);
    }

    public static  Set<Enum<HsBookingItemType>> types() {
        return validators.keySet();
    }

    public static List<String> doValidate(final EntityManager em, final HsBookingItem bookingItem) {
        return HsEntityValidator.doWithEntityManager(em, () ->
            HsEntityValidator.sequentiallyValidate(
                    () -> HsBookingItemEntityValidatorRegistry.forType(bookingItem.getType()).validateEntity(bookingItem),
                    () -> HsBookingItemEntityValidatorRegistry.forType(bookingItem.getType()).validateContext(bookingItem))
        );
    }

    public static <E extends HsBookingItem> E validated(final EntityManager em, final E entityToSave) {
        MultiValidationException.throwIfNotEmpty(doValidate(em, entityToSave));
        return entityToSave;
    }
}
