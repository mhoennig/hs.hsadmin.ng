package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedAppEvent;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedEventEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.Dns;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContact;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapperFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static net.hostsharing.hsadminng.mapper.PatchableMapWrapper.entry;
import static org.assertj.core.api.Assertions.assertThat;

// Tests the DomainSetupHostingAssetFactory through a HsBookingItemCreatedListener instance.
@ExtendWith(MockitoExtension.class)
class ManagedWebspaceHostingAssetFactoryUnitTest {

    final HsBookingDebitorEntity debitor = HsBookingDebitorEntity.builder()
            .debitorNumber(12345)
            .defaultPrefix("xyz")
            .build();
    final HsBookingProjectRealEntity project = HsBookingProjectRealEntity.builder()
            .debitor(debitor)
            .caption("Test-Project")
            .build();
    final HsOfficeContact alarmContact = HsOfficeContactRealEntity.builder()
            .uuid(UUID.randomUUID())
            .caption("Alarm Contact xyz")
            .build();

    private EntityManagerWrapperFake emwFake = new EntityManagerWrapperFake();

    @Spy
    private EntityManagerWrapper emw = emwFake;

    @Spy
    private ObjectMapper jsonMapper = new JsonObjectMapperConfiguration().customObjectMapper().build();

    @Spy
    private StrictMapper StrictMapper = new StrictMapper(emw);

    @InjectMocks
    private HsBookingItemCreatedListener listener;

    @BeforeEach
    void initMocks() {
        emwFake.persist(alarmContact);
    }

    @Test
    void doesNotPersistAnyEntityWithoutHostingAssetWithoutValidationErrors() {
        // given
        final var givenBookingItem = HsBookingItemRealEntity.builder()
                .type(HsBookingItemType.MANAGED_WEBSPACE)
                .project(project)
                .caption("Test Managed-Webspace")
                .resources(Map.ofEntries(
                        Map.entry("RAM", 25),
                        Map.entry("Traffic", 250)
                ))
                .build();

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, null)
        );

        // then
        assertThat(emwFake.stream(BookingItemCreatedEventEntity.class).findAny().isEmpty())
                .as("the event should not have been persisted, but got persisted").isTrue();
        assertThat(emwFake.stream(HsHostingAssetRealEntity.class).findAny().isEmpty())
                .as("the hosting asset should not have been persisted, but got persisted").isTrue();
    }

    @Test
    void persistsEventEntityIfDomainSetupVerificationFails() {
        // given
        final var givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org")
        );
        final var givenAssetJson = """
                {
                    "identifier": "xyz00"
                }
                """;
        Dns.fakeResultForDomain("example.org", Dns.Result.fromRecords()); // without valid verificationCode

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)
        );

        // then
        assertEventStatus(givenBookingItem, givenAssetJson,
                "requires MANAGED_WEBSPACE hosting asset, but got null");
    }

    @SafeVarargs
    private static HsBookingItemRealEntity createBookingItemFromResources(final Map.Entry<String, String>... givenResources) {
        return HsBookingItemRealEntity.builder()
                .type(HsBookingItemType.MANAGED_WEBSPACE)
                .resources(Map.ofEntries(givenResources))
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
