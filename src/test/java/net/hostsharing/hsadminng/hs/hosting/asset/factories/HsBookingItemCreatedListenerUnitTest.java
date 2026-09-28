package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import lombok.val;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedAppEvent;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedEventEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapperFake;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import jakarta.validation.ValidationException;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class HsBookingItemCreatedListenerUnitTest {

    final HsBookingDebitorEntity debitor = HsBookingDebitorEntity.builder()
            .debitorNumber(12345)
            .defaultPrefix("xyz")
            .build();

    private EntityManagerWrapperFake emwFake = new EntityManagerWrapperFake();

    @Spy
    private EntityManagerWrapper emw = emwFake;

    @Spy
    private ObjectMapper jsonMapper = new JsonObjectMapperConfiguration().customObjectMapper();

    @Spy
    private StrictMapper StrictMapper = new StrictMapper(emw);

    @InjectMocks
    private HsBookingItemCreatedListener listener;

    @ParameterizedTest
    @MethodSource("bookingItemTypesWithoutAutomaticAssetCreation")
    void persistsEventEntityIfBookingItemTypeDoesNotSupportAutomaticHostingAssetCreation(final HsBookingItemType bookingItemType) {
        // given
        final var givenBookingItem = createBookingItemFromResources(bookingItemType);
        final var givenAssetJson = """
                {
                    // anything should be rejected
                }
                """;

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)
        );

        // then
        assertEventStatus(givenBookingItem, givenAssetJson,
                "waiting for manual setup of hosting asset for booking item of type " + bookingItemType);
    }

    @ParameterizedTest
    @MethodSource("bookingItemTypesWithoutAutomaticAssetCreation")
    void persistsEventEntityIfBookingItemTypeNeedsManualSetupEvenWithoutHostingAsset(final HsBookingItemType bookingItemType) {
        // given
        val givenBookingItem = createBookingItemFromResources(bookingItemType);

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, null)
        );

        // then
        assertEventStatus(givenBookingItem, null,
                "waiting for manual setup of hosting asset for booking item of type " + bookingItemType);
    }

    @ParameterizedTest
    @MethodSource("assetJsonWithoutSubHostingAssets")
    void rejectsDomainSetupAssetJsonWithoutSubHostingAssets(final String givenAssetJson) {
        // given
        val givenBookingItem = createBookingItemFromResources(DOMAIN_SETUP);

        // when
        val thrown = catchThrowable(() -> listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)));

        // then
        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessageContaining("'hostingAsset.subHostingAssets'");
    }

    static List<String> assetJsonWithoutSubHostingAssets() {
        return List.of(
                """
                {
                    "identifier": "example.org"
                }
                """,
                """
                {
                    "identifier": "example.org",
                    "subHostingAssets": []
                }
                """);
    }

    static List<HsBookingItemType> bookingItemTypesWithoutAutomaticAssetCreation() {
        return Arrays.stream(HsBookingItemType.values())
                .filter(v -> v != MANAGED_WEBSPACE && v != DOMAIN_SETUP)
                .toList();
    }

    private static HsBookingItemRealEntity createBookingItemFromResources(
            final HsBookingItemType bookingItemType
    ) {
        return HsBookingItemRealEntity.builder()
                .type(bookingItemType)
                .build();
    }

    private void assertEventStatus(
            final HsBookingItemRealEntity givenBookingItem,
            final String givenAssetJson,
            final String expectedErrorMessage) {
        assertThat(emwFake.stream(BookingItemCreatedEventEntity.class).reduce(Reducer::toSingleElement))
                .as("the event should have been persisted, but did not get persisted")
                .hasValueSatisfying(eventEntity -> {
                    assertThat(eventEntity.getBookingItem()).isSameAs(givenBookingItem);
                    assertThat(eventEntity.getAssetJson()).isEqualTo(givenAssetJson);
                    assertThat(eventEntity.getStatusMessage()).isEqualTo(expectedErrorMessage);
                });
    }
}
