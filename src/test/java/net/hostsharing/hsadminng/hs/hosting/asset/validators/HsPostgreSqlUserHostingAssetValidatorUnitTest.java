package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hash.HashGenerator;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.stream.Stream;

import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_INSTANCE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.PGSQL_USER;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_SERVER_HOSTING_ASSET;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_WEBSPACE_HOSTING_ASSET;
import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static org.assertj.core.api.Assertions.assertThat;

class HsPostgreSqlUserHostingAssetValidatorUnitTest {

    private static final HsHostingAssetEntity GIVEN_PGSQL_INSTANCE = HsHostingAssetEntity.builder()
            .type(PGSQL_INSTANCE)
            .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
            .identifier("vm1234|PgSql.default")
            .caption("some valid test PgSql-Instance")
            .build();

    private EntityManager em = null; // not actually needed in these test cases

    private static HsHostingAssetEntityBuilder givenValidMariaDbUserBuilder() {
        return HsHostingAssetEntity.builder()
                .type(PGSQL_USER)
                .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
                .assignedToAsset(GIVEN_PGSQL_INSTANCE)
                .identifier("xyz00_temp")
                .caption("some valid test PgSql-User")
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
                "{type=password, propertyName=password, minLength=8, maxLength=40, writeOnly=true, computed=IN_PREP, hashedUsing=SCRAM_SHA256, undisclosed=true}"
        );
    }

    @Test
    void preparesEntity() {
        // given
        final var givenMariaDbUserHostingAsset = givenValidMariaDbUserBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenMariaDbUserHostingAsset.getType());

        // when
        HashGenerator.nextSalt(new String(Base64.getDecoder().decode("L1QxSVNyTU81b3NZS1djNg=="), Charset.forName("latin1")));
        validator.prepareProperties(em, givenMariaDbUserHostingAsset);

        // then
        assertThat(givenMariaDbUserHostingAsset.getConfig()).containsExactlyInAnyOrderEntriesOf(ofEntries(
                entry("password", "SCRAM-SHA-256$4096:L1QxSVNyTU81b3NZS1djNg==$bB4PEqHpnkoB9FwYfOjh+8yJvLsCnrwxom3TGK0CVJM=:ACRgTfhJwIZLrzhVRbJ3Qif5YhErYWAfkBThvtouW+8=")
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
                "'PGSQL_USER:xyz00_temp.config.unknown' is not expected but is set to '100'",
                "'PGSQL_USER:xyz00_temp.config.password' length is expected to be at min 8 but length of provided value is 5",
                "'PGSQL_USER:xyz00_temp.config.password' must contain at least one character of at least 3 of the following groups: upper case letters, lower case letters, digits, special characters"
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
