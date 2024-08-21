package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.EntityManagerMock;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.stream.Stream;

import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetTestEntities.MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetTestEntities.MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_DATABASE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_USER;
import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HsPostgreSqlDatabaseHostingAssetValidatorUnitTest {

    private static final HsHostingAssetRealEntity GIVEN_PGSQL_INSTANCE = HsHostingAssetRealEntity.builder()
            .type(PGSQL_INSTANCE)
            .parentAsset(MANAGED_SERVER_HOSTING_ASSET_REAL_TEST_ENTITY)
            .identifier("vm1234|PgSql.default")
            .caption("some valid test PgSql-Instance")
            .build();

    private static final HsHostingAssetRealEntity GIVEN_PGSQL_USER = HsHostingAssetRealEntity.builder()
            .type(PGSQL_USER)
            .parentAsset(MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY)
            .assignedToAsset(GIVEN_PGSQL_INSTANCE)
            .identifier("xyz00_user")
            .caption("some valid test PgSql-User")
            .config(new HashMap<>(ofEntries(
                    entry("password", "Hallo Datenbank, lass mich rein!")
            )))
            .build();

    private static HsHostingAssetRbacEntity.HsHostingAssetRbacEntityBuilder<?, ?> givenValidPgSqlDatabaseBuilder() {
        return HsHostingAssetRbacEntity.builder()
                .type(PGSQL_DATABASE)
                .parentAsset(GIVEN_PGSQL_USER)
                .identifier("PGD|xyz00_db")
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
        final var em = EntityManagerMock.createEntityManagerMockWithAssetQueryFake(null);

        // when
        final var result = HsEntityValidator.doWithEntityManager(em, () -> Stream.concat(
                validator.validateEntity(givenPgSqlUserHostingAsset).stream(),
                validator.validateContext(givenPgSqlUserHostingAsset).stream()
        ).toList());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidReferences() {
        // given
        final var givenPgSqlUserHostingAsset = givenValidPgSqlDatabaseBuilder()
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetRealEntity.builder().type(PGSQL_INSTANCE).build())
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(PGSQL_INSTANCE).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenPgSqlUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(givenPgSqlUserHostingAsset);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'PGSQL_DATABASE:PGD|xyz00_db.bookingItem' must be null but is of type CLOUD_SERVER",
                "'PGSQL_DATABASE:PGD|xyz00_db.parentAsset' must be of type PGSQL_USER but is of type PGSQL_INSTANCE",
                "'PGSQL_DATABASE:PGD|xyz00_db.assignedToAsset' must be null but is of type PGSQL_INSTANCE"
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
                "'PGSQL_DATABASE:PGD|xyz00_db.config.unknown' is not expected but is set to 'wrong'",
                "'PGSQL_DATABASE:PGD|xyz00_db.config.encoding' is expected to be of type String, but is of type Integer"
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
                "'identifier' expected to match '^PGD\\|xyz00$|^PGD\\|xyz00_[a-zA-Z0-9_]+$', but is 'xyz99-temp'");
    }
}
