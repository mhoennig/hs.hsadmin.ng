package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import lombok.experimental.UtilityClass;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;

@UtilityClass
public class HsHostingAssetEntityValidators {

    private static final Map<Enum<HsHostingAssetType>, HsEntityValidator<HsHostingAssetEntity, HsHostingAssetType>> validators = new HashMap<>();
    static {
        register(CLOUD_SERVER, new HsCloudServerHostingAssetValidator());
        register(MANAGED_SERVER, new HsManagedServerHostingAssetValidator());
        register(MANAGED_WEBSPACE, new HsManagedWebspaceHostingAssetValidator());
    }

    private static void register(final Enum<HsHostingAssetType> type, final HsEntityValidator<HsHostingAssetEntity, HsHostingAssetType> validator) {
        stream(validator.propertyValidators).forEach( entry -> {
            entry.verifyConsistency(Map.entry(type, validator));
        });
        validators.put(type, validator);
    }

    public static HsEntityValidator<HsHostingAssetEntity, HsHostingAssetType> forType(final Enum<HsHostingAssetType> type) {
        return validators.get(type);
    }

    public static Set<Enum<HsHostingAssetType>> types() {
        return validators.keySet();
    }


    public static HsHostingAssetEntity valid(final HsHostingAssetEntity entityToSave) {
        final var violations = HsHostingAssetEntityValidators.forType(entityToSave.getType()).validate(entityToSave);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.toString());
        }
        return entityToSave;
    }
}
