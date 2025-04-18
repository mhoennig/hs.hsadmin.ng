package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HostingAssetEntityValidatorRegistryUnitTest {

    @Test
    void forTypeWithUnknownTypeThrowsException() {
        // when
        final var thrown = catchThrowable(() -> {
            HostingAssetEntityValidatorRegistry.forType(null);
        });

        // then
        assertThat(thrown).hasMessage("no validator found for type null");
    }

    @Test
    void typesReturnsAllImplementedTypes() {
        // when
        final var types = HostingAssetEntityValidatorRegistry.types();

        // then
        // TODO.test: when all types are implemented, replace with set of all types:
        // assertThat(types).isEqualTo(EnumSet.allOf(HsHostingAssetType.class));
        // also remove "Implemented" from the test method name.
        assertThat(types).containsExactlyInAnyOrder(
                HsHostingAssetType.CLOUD_SERVER,
                HsHostingAssetType.MANAGED_SERVER,
                HsHostingAssetType.MANAGED_WEBSPACE,
                HsHostingAssetType.UNIX_USER,
                HsHostingAssetType.EMAIL_ALIAS,
                HsHostingAssetType.DOMAIN_SETUP,
                HsHostingAssetType.DOMAIN_DNS_SETUP,
                HsHostingAssetType.DOMAIN_HTTP_SETUP,
                HsHostingAssetType.DOMAIN_SMTP_SETUP,
                HsHostingAssetType.DOMAIN_MBOX_SETUP,
                HsHostingAssetType.EMAIL_ADDRESS,
                HsHostingAssetType.MARIADB_INSTANCE,
                HsHostingAssetType.MARIADB_USER,
                HsHostingAssetType.MARIADB_DATABASE,
                HsHostingAssetType.PGSQL_INSTANCE,
                HsHostingAssetType.PGSQL_USER,
                HsHostingAssetType.PGSQL_DATABASE,
                HsHostingAssetType.IPV4_NUMBER,
                HsHostingAssetType.IPV6_NUMBER
        );
    }
}
