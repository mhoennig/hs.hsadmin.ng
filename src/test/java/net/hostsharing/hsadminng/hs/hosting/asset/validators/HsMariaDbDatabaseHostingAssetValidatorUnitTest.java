package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.stream.Stream;

import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_DATABASE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MARIADB_USER;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_SERVER_HOSTING_ASSET;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_WEBSPACE_HOSTING_ASSET;
import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static org.assertj.core.api.Assertions.assertThat;

class HsMariaDbDatabaseHostingAssetValidatorUnitTest {

    private static final HsHostingAssetEntity GIVEN_MARIADB_INSTANCE = HsHostingAssetEntity.builder()
            .type(MARIADB_INSTANCE)
            .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
            .identifier("vm1234|MariaDB.default")
            .caption("some valid test MariaDB-Instance")
            .build();

    private static final HsHostingAssetEntity GIVEN_MARIADB_USER = HsHostingAssetEntity.builder()
            .type(MARIADB_USER)
            .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
            .assignedToAsset(GIVEN_MARIADB_INSTANCE)
            .identifier("xyz00_temp")
            .caption("some valid test MariaDB-User")
            .config(new HashMap<>(ofEntries(
                    entry("password", "Hallo Datenbank, lass mich rein!")
            )))
            .build();

    private static HsHostingAssetEntityBuilder givenValidMariaDbDatabaseBuilder() {
        return HsHostingAssetEntity.builder()
                .type(MARIADB_DATABASE)
                .parentAsset(GIVEN_MARIADB_USER)
                .identifier("MAD|xyz00_temp")
                .caption("some valid test MariaDB-Database")
                .config(new HashMap<>(ofEntries(
                        entry("encoding", "latin1")
                )));
    }

    @Test
    void describesItsProperties() {
        // given
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenValidMariaDbDatabaseBuilder().build().getType());

        // when
        final var props = validator.properties();

        // then
        assertThat(props).extracting(Object::toString).containsExactlyInAnyOrder(
                "{type=string, propertyName=encoding, matchesRegEx=[[a-z0-9_]+], maxLength=24, provided=[latin1, utf8], defaultValue=utf8}"
        );
    }

    @Test
    void validatesValidEntity() {
        // given
        final var givenMariaDbUserHostingAsset = givenValidMariaDbDatabaseBuilder().build();
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
        final var givenMariaDbUserHostingAsset = givenValidMariaDbDatabaseBuilder()
                .config(ofEntries(
                        entry("unknown", "wrong"),
                        entry("encoding", 10)
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenMariaDbUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(givenMariaDbUserHostingAsset);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'MARIADB_DATABASE:MAD|xyz00_temp.config.unknown' is not expected but is set to 'wrong'",
                "'MARIADB_DATABASE:MAD|xyz00_temp.config.encoding' is expected to be of type String, but is of type Integer"
        );
    }

    @Test
    void rejectsInvalidIdentifier() {
        // given
        final var givenMariaDbUserHostingAsset = givenValidMariaDbDatabaseBuilder()
                .identifier("xyz99-temp")
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenMariaDbUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(givenMariaDbUserHostingAsset);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^MAD\\|xyz00$|^MAD\\|xyz00_[a-zA-Z0-9_]+$', but is 'xyz99-temp'");
    }
}
