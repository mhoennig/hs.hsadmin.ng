package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetResource;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import java.util.*;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.*;

public class HostingAssetEntityValidatorRegistry {

    private static final Map<Enum<HsHostingAssetType>, HsEntityValidator<HsHostingAsset>> validators = new HashMap<>();
    static {
        // HOWTO: add (register) new HsHostingAssetType-specific validators
        register(CLOUD_SERVER, new HsCloudServerHostingAssetValidator());
        register(MANAGED_SERVER, new HsManagedServerHostingAssetValidator());
        register(MANAGED_WEBSPACE, new HsManagedWebspaceHostingAssetValidator());
        register(UNIX_USER, new HsUnixUserHostingAssetValidator());
        register(EMAIL_ALIAS, new HsEMailAliasHostingAssetValidator());
        register(DOMAIN_SETUP, new HsDomainSetupHostingAssetValidator());
        register(DOMAIN_DNS_SETUP, new HsDomainDnsSetupHostingAssetValidator());
        register(DOMAIN_HTTP_SETUP, new HsDomainHttpSetupHostingAssetValidator());
        register(DOMAIN_SMTP_SETUP, new HsDomainSmtpSetupHostingAssetValidator());
        register(DOMAIN_MBOX_SETUP, new HsDomainMboxSetupHostingAssetValidator());
        register(EMAIL_ADDRESS, new HsEMailAddressHostingAssetValidator());
        register(MARIADB_INSTANCE, new HsMariaDbInstanceHostingAssetValidator());
        register(MARIADB_USER, new HsMariaDbUserHostingAssetValidator());
        register(MARIADB_DATABASE, new HsMariaDbDatabaseHostingAssetValidator());
        register(PGSQL_INSTANCE, new HsPostgreSqlDbInstanceHostingAssetValidator());
        register(PGSQL_USER, new HsPostgreSqlUserHostingAssetValidator());
        register(PGSQL_DATABASE, new HsPostgreSqlDatabaseHostingAssetValidator());
        register(IPV4_NUMBER, new HsIPv4NumberHostingAssetValidator());
        register(IPV6_NUMBER, new HsIPv6NumberHostingAssetValidator());
    }

    private static void register(final Enum<HsHostingAssetType> type, final HsEntityValidator<HsHostingAsset> validator) {
        stream(validator.propertyValidators).forEach( entry -> {
            entry.verifyConsistency(Map.entry(type, validator));
        });
        validators.put(type, validator);
    }

    public static HsEntityValidator<HsHostingAsset> forType(final Enum<HsHostingAssetType> type) {
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
