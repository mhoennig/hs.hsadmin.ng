package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.stream.Stream;

import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_USER;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_SERVER_HOSTING_ASSET;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_WEBSPACE_HOSTING_ASSET;
import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static org.assertj.core.api.Assertions.assertThat;

class HsMariaDbUserHostingAssetValidatorUnitTest {

    private static final HsHostingAssetEntity GIVEN_MARIADB_INSTANCE = HsHostingAssetEntity.builder()
            .type(MARIADB_INSTANCE)
            .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
            .identifier("vm1234|MariaDB.default")
            .caption("some valid test MariaDB-Instance")
            .build();

    private EntityManager em = null; // not actually needed in these test cases

    private static HsHostingAssetEntityBuilder givenValidMariaDbUserBuilder() {
        return HsHostingAssetEntity.builder()
                .type(MARIADB_USER)
                .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
                .assignedToAsset(GIVEN_MARIADB_INSTANCE)
                .identifier("xyz00_temp")
                .caption("some valid test MariaDB-User")
                .config(new HashMap<>(ofEntries(
                        entry("password", "Test1234")
                )));
    }

    @Test
    void describesItsProperties() {
        // given
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenValidMariaDbUserBuilder().build().getType());

        // when
        final var props = validator.properties();

        // then
        assertThat(props).extracting(Object::toString).containsExactlyInAnyOrder(
                "{type=password, propertyName=password, minLength=8, maxLength=40, writeOnly=true, computed=IN_PREP, hashedUsing=MYSQL_NATIVE, undisclosed=true}"
        );
    }

    @Test
    void preparesEntity() {
        // given
        final var givenMariaDbUserHostingAsset = givenValidMariaDbUserBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenMariaDbUserHostingAsset.getType());

        // when
        // HashGenerator.nextSalt("Ly3LbsArtL5u4EVt"); // not needed for mysql_native_password
        validator.prepareProperties(em, givenMariaDbUserHostingAsset);

        // then
        assertThat(givenMariaDbUserHostingAsset.getConfig()).containsExactlyInAnyOrderEntriesOf(ofEntries(
                entry("password", "*14F1A8C42F8B6D4662BB3ED290FD37BF135FE45C")
        ));
    }

    @Test
    void validatesValidEntity() {
        // given
        final var givenMariaDbUserHostingAsset = givenValidMariaDbUserBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenMariaDbUserHostingAsset.getType());

        // when
        final var result = Stream.concat(
                validator.validateEntity(givenMariaDbUserHostingAsset).stream(),
                validator.validateContext(givenMariaDbUserHostingAsset).stream()
        ).toList();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidProperties() {
        // given
        final var givenMariaDbUserHostingAsset = givenValidMariaDbUserBuilder()
                .config(ofEntries(
                        entry("unknown", 100),
                        entry("password", "short")
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenMariaDbUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(givenMariaDbUserHostingAsset);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MARIADB_USER:xyz00_temp.config.unknown' is not expected but is set to '100'",
                "'MARIADB_USER:xyz00_temp.config.password' length is expected to be at min 8 but length of provided value is 5",
                "'MARIADB_USER:xyz00_temp.config.password' must contain at least one character of at least 3 of the following groups: upper case letters, lower case letters, digits, special characters"
        );
    }

    @Test
    void rejectsInvalidIdentifier() {
        // given
        final var givenMariaDbUserHostingAsset = givenValidMariaDbUserBuilder()
                .identifier("xyz99-temp")
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenMariaDbUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(givenMariaDbUserHostingAsset);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^xyz00$|^xyz00_[a-z0-9_]+$', but is 'xyz99-temp'");
    }
}
