package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedAppEvent;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedEventEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapperFake;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;

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
    private ObjectMapper jsonMapper = new JsonObjectMapperConfiguration().customObjectMapper().build();

    @Spy
    private StandardMapper standardMapper = new StandardMapper(emw);

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
        emwFake.stream(BookingItemCreatedEventEntity.class)
                .reduce(Reducer::toSingleElement)
                .map(eventEntity -> {
                    assertThat(eventEntity.getBookingItem()).isSameAs(givenBookingItem);
                    assertThat(eventEntity.getAssetJson()).isEqualTo(givenAssetJson);
                    assertThat(eventEntity.getStatusMessage()).isEqualTo(expectedErrorMessage);
                    return true;
                });
    }
}
