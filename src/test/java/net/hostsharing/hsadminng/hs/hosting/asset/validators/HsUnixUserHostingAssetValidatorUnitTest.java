package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hash.HashGenerator;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.stream.Stream;

import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_MANAGED_SERVER_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_MANAGED_WEBSPACE_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class HsUnixUserHostingAssetValidatorUnitTest {

    private final HsHostingAssetEntity TEST_MANAGED_SERVER_HOSTING_ASSET = HsHostingAssetEntity.builder()
            .type(HsHostingAssetType.MANAGED_SERVER)
            .identifier("vm1234")
            .caption("some managed server")
            .bookingItem(TEST_MANAGED_SERVER_BOOKING_ITEM)
            .build();
    private final HsHostingAssetEntity TEST_MANAGED_WEBSPACE_HOSTING_ASSET = HsHostingAssetEntity.builder()
            .type(MANAGED_WEBSPACE)
            .bookingItem(TEST_MANAGED_WEBSPACE_BOOKING_ITEM)
            .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
            .identifier("abc00")
            .build();
    private final HsHostingAssetEntity GIVEN_VALID_UNIX_USER_HOSTING_ASSET = HsHostingAssetEntity.builder()
            .type(UNIX_USER)
            .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
            .identifier("abc00-temp")
            .caption("some valid test UnixUser")
            .config(new HashMap<>(ofEntries(
                    entry("SSD hard quota", 50),
                    entry("SSD soft quota", 40),
                    entry("totpKey", "0x123456789abcdef01234"),
                    entry("password", "Hallo Computer, lass mich rein!")
            )))
            .build();

    @Mock
    EntityManager em;

    @BeforeEach
    void initMocks() {
        final var nativeQueryMock = mock(Query.class);
        lenient().when(nativeQueryMock.getSingleResult()).thenReturn(12345678);
        lenient().when(em.createNativeQuery("SELECT nextval('hs_hosting_asset_unixuser_system_id_seq')", Integer.class))
                .thenReturn(nativeQueryMock);

    }

    @Test
    void preparesUnixUser() {
        // given
        final var unixUserHostingAsset =  GIVEN_VALID_UNIX_USER_HOSTING_ASSET;
        final var validator = HostingAssetEntityValidatorRegistry.forType(unixUserHostingAsset.getType());

        // when
        HashGenerator.nextSalt("Ly3LbsArtL5u4EVt");
        validator.prepareProperties(em, unixUserHostingAsset);

        // then
        assertThat(unixUserHostingAsset.getConfig()).containsExactlyInAnyOrderEntriesOf(ofEntries(
                entry("SSD hard quota", 50),
                entry("SSD soft quota", 40),
                entry("totpKey", "0x123456789abcdef01234"),
                entry("password", "$6$Ly3LbsArtL5u4EVt$i/ayIEvm0y4bjkFB6wbg8imbRIaw4mAA4gqYRVyoSkj.iIxJKS3KiRkSjP8gweNcpKL0Q0N31EadT8fCnWErL."),
                entry("userid", 12345678)
        ));
    }

    @Test
    void validatesValidUnixUser() {
        // given
        final var unixUserHostingAsset =  GIVEN_VALID_UNIX_USER_HOSTING_ASSET;
        final var validator = HostingAssetEntityValidatorRegistry.forType(unixUserHostingAsset.getType());

        // when
        final var result = Stream.concat(
                validator.validateEntity(unixUserHostingAsset).stream(),
                validator.validateContext(unixUserHostingAsset).stream()
        ).toList();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validatesUnixUserProperties() {
        // given
        final var unixUserHostingAsset = HsHostingAssetEntity.builder()
                .type(UNIX_USER)
                .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
                .identifier("abc00-temp")
                .caption("some test UnixUser with invalid properties")
                .config(ofEntries(
                        entry("SSD hard quota", 60000),
                        entry("SSD soft quota", 70000),
                        entry("HDD hard quota", 100),
                        entry("HDD soft quota", 200),
                        entry("shell", "/is/invalid"),
                        entry("homedir", "/is/read-only"),
                        entry("totpKey", "should be a hex number"),
                        entry("password", "short")
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(unixUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(unixUserHostingAsset);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'UNIX_USER:abc00-temp.config.SSD hard quota' is expected to be at most 51200 but is 60000",
                "'UNIX_USER:abc00-temp.config.SSD soft quota' is expected to be at most 60000 but is 70000",
                "'UNIX_USER:abc00-temp.config.HDD hard quota' is expected to be at most 0 but is 100",
                "'UNIX_USER:abc00-temp.config.HDD soft quota' is expected to be at most 100 but is 200",
                "'UNIX_USER:abc00-temp.config.homedir' is readonly but given as '/is/read-only'",
                "'UNIX_USER:abc00-temp.config.totpKey' is expected to match any of [^0x([0-9A-Fa-f]{2})+$] but provided value does not match",
                "'UNIX_USER:abc00-temp.config.password' length is expected to be at min 8 but length of provided value is 5",
                "'UNIX_USER:abc00-temp.config.password' must contain at least one character of at least 3 of the following groups: upper case letters, lower case letters, digits, special characters"
        );
    }

    @Test
    void validatesInvalidIdentifier() {
        // given
        final var unixUserHostingAsset = HsHostingAssetEntity.builder()
                .type(UNIX_USER)
                .parentAsset(HsHostingAssetEntity.builder().type(MANAGED_WEBSPACE).identifier("abc00").build())
                .identifier("xyz99-temp")
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(unixUserHostingAsset.getType());

        // when
        final var result = validator.validateEntity(unixUserHostingAsset);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^abc00$|^abc00-[a-z0-9\\._-]+$', but is 'xyz99-temp'");
    }

    @Test
    void revampsUnixUser() {
        // given
        final var unixUserHostingAsset =  GIVEN_VALID_UNIX_USER_HOSTING_ASSET;
        final var validator = HostingAssetEntityValidatorRegistry.forType(unixUserHostingAsset.getType());

        // when
        HashGenerator.nextSalt("Ly3LbsArtL5u4EVt");
        final var result = validator.revampProperties(em, unixUserHostingAsset, unixUserHostingAsset.getConfig());

        // then
        assertThat(result).containsExactlyInAnyOrderEntriesOf(ofEntries(
                entry("SSD hard quota", 50),
                entry("SSD soft quota", 40),
                entry("homedir", "/home/pacs/abc00/users/temp")
        ));
    }

    @Test
    void describesItsProperties() {
        // given
        final var validator = HostingAssetEntityValidatorRegistry.forType(UNIX_USER);

        // when
        final var props = validator.properties();

        // then
        assertThat(props).extracting(Object::toString).containsExactlyInAnyOrder(
                "{type=boolean, propertyName=locked, readOnly=true}",
                "{type=integer, propertyName=userid, readOnly=true, computed=IN_INIT}",
                "{type=integer, propertyName=SSD hard quota, unit=MB, maxFrom=SSD}",
                "{type=integer, propertyName=SSD soft quota, unit=MB, maxFrom=SSD hard quota}",
                "{type=integer, propertyName=HDD hard quota, unit=MB, maxFrom=HDD}",
                "{type=integer, propertyName=HDD soft quota, unit=MB, maxFrom=HDD hard quota}",
                "{type=string, propertyName=shell, provided=[/bin/false, /bin/bash, /bin/csh, /bin/dash, /usr/bin/tcsh, /usr/bin/zsh, /usr/bin/passwd], defaultValue=/bin/false}",
                "{type=string, propertyName=homedir, readOnly=true, computed=IN_REVAMP}",
                "{type=string, propertyName=totpKey, matchesRegEx=[^0x([0-9A-Fa-f]{2})+$], minLength=20, maxLength=256, writeOnly=true, undisclosed=true}",
                "{type=password, propertyName=password, minLength=8, maxLength=40, writeOnly=true, computed=IN_PREP, hashedUsing=LINUX_SHA512, undisclosed=true}"
        );
    }
}
