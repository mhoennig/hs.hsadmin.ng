package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.stream.Stream;

import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_DATABASE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_USER;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_SERVER_HOSTING_ASSET;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_WEBSPACE_HOSTING_ASSET;
import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static org.assertj.core.api.Assertions.assertThat;

class HsPostgreSqlDatabaseHostingAssetValidatorUnitTest {

    private static final HsHostingAssetEntity GIVEN_PGSQL_INSTANCE = HsHostingAssetEntity.builder()
            .type(PGSQL_INSTANCE)
            .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
            .identifier("vm1234|PgSql.default")
            .caption("some valid test PgSql-Instance")
            .build();

    private static final HsHostingAssetEntity GIVEN_PGSQL_USER = HsHostingAssetEntity.builder()
            .type(PGSQL_USER)
            .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
            .assignedToAsset(GIVEN_PGSQL_INSTANCE)
            .identifier("xyz00_temp")
            .caption("some valid test PgSql-User")
            .config(new HashMap<>(ofEntries(
                    entry("password", "Hallo Datenbank, lass mich rein!")
            )))
            .build();

    private static HsHostingAssetEntityBuilder givenValidPgSqlDatabaseBuilder() {
        return HsHostingAssetEntity.builder()
                .type(PGSQL_DATABASE)
                .parentAsset(GIVEN_PGSQL_USER)
                .identifier("xyz00_temp")
                .caption("some valid test PgSql-Database")
                .config(new HashMap<>(ofEntries(
                        entry("encoding", "LATIN1")
                )));
    }

    @Test
    void describesItsProperties() {
        // given
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenValidPgSqlDatabaseBuilder().build().getType());

        // when
        final var props = validator.properties();

        // then
        assertThat(props).extracting(Object::toString).containsExactlyInAnyOrder(
                "{type=string, propertyName=encoding, matchesRegEx=[[A-Z0-9_]+], maxLength=24, provided=[LATIN1, UTF8], defaultValue=UTF8}"
        );
    }

    @Test
    void validatesValidEntity() {
        // given
        final var givenPgSqlUserHostingAsset = givenValidPgSqlDatabaseBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenPgSqlUserHostingAsset.getType());

        // when
        final var result = Stream.concat(
                validator.validateEntity(givenPgSqlUserHostingAsset).stream(),
                validator.validateContext(givenPgSqlUserHostingAsset).stream()
        ).toList();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidReferences() {
        // given
        final var givenPgSqlUserHostingAsset = givenValidPgSqlDatabaseBuilder()
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetEntity.builder().type(PGSQL_INSTANCE).build())
                .assignedToAsset(HsHostingAssetEntity.builder().type(PGSQL_INSTANCE).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenPgSqlUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(givenPgSqlUserHostingAsset);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'PGSQL_DATABASE:xyz00_temp.config.unknown' is not expected but is set to 'wrong'",
                "'PGSQL_DATABASE:xyz00_temp.config.encoding' is expected to be of type String, but is of type Integer"
        );
    }

    @Test
    void rejectsInvalidProperties() {
        // given
        final var givenPgSqlUserHostingAsset = givenValidPgSqlDatabaseBuilder()
                .config(ofEntries(
                        entry("unknown", "wrong"),
                        entry("encoding", 10)
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenPgSqlUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(givenPgSqlUserHostingAsset);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'PGSQL_DATABASE:xyz00_temp.config.unknown' is not expected but is set to 'wrong'",
                "'PGSQL_DATABASE:xyz00_temp.config.encoding' is expected to be of type String, but is of type Integer"
        );
    }

    @Test
    void rejectsInvalidIdentifier() {
        // given
        final var givenPgSqlUserHostingAsset = givenValidPgSqlDatabaseBuilder()
                .identifier("xyz99-temp")
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenPgSqlUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(givenPgSqlUserHostingAsset);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^xyz00$|^xyz00_[a-z0-9_]+$', but is 'xyz99-temp'");
    }
}
