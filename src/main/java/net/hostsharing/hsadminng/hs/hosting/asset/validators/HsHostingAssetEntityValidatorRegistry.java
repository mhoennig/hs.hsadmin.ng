package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetResource;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import java.util.*;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.*;

public class HsHostingAssetEntityValidatorRegistry {

    private static final Map<Enum<HsHostingAssetType>, HsEntityValidator<HsHostingAssetEntity>> validators = new HashMap<>();
    static {
        // HOWTO: add (register) new HsHostingAssetType-specific validators
        register(CLOUD_SERVER, new HsCloudServerHostingAssetValidator());
        register(MANAGED_SERVER, new HsManagedServerHostingAssetValidator());
        register(MANAGED_WEBSPACE, new HsManagedWebspaceHostingAssetValidator());
        register(UNIX_USER, new HsUnixUserHostingAssetValidator());
    }

    private static void register(final Enum<HsHostingAssetType> type, final HsEntityValidator<HsHostingAssetEntity> validator) {
        stream(validator.propertyValidators).forEach( entry -> {
            entry.verifyConsistency(Map.entry(type, validator));
        });
        validators.put(type, validator);
    }

    public static HsEntityValidator<HsHostingAssetEntity> forType(final Enum<HsHostingAssetType> type) {
        if ( validators.containsKey(type)) {
            return validators.get(type);
        }
        throw new IllegalArgumentException("no validator found for type " + type);
    }

    public static Set<Enum<HsHostingAssetType>> types() {
        return validators.keySet();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(final HsHostingAssetResource resource) {
        if (resource.getConfig() instanceof Map map) {
            return map;
        }
        throw new IllegalArgumentException("expected a Map, but got a " + resource.getConfig().getClass());
    }

}
