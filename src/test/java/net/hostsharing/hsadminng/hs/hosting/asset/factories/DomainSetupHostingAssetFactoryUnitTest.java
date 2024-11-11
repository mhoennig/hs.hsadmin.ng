package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedAppEvent;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedEventEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.Dns;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapperFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static net.hostsharing.hsadminng.mapper.PatchableMapWrapper.entry;
import static org.assertj.core.api.Assertions.assertThat;

// Tests the DomainSetupHostingAssetFactory through a HsBookingItemCreatedListener instance.
@ExtendWith(MockitoExtension.class)
class DomainSetupHostingAssetFactoryUnitTest {

    private final HsHostingAssetRealEntity managedWebspaceHostingAsset = HsHostingAssetRealEntity.builder()
            .uuid(UUID.randomUUID())
            .type(MANAGED_WEBSPACE)
            .identifier("one00")
            .build();

    private final HsHostingAssetRealEntity unixUserHostingAsset = HsHostingAssetRealEntity.builder()
            .uuid(UUID.randomUUID())
            .type(UNIX_USER)
            .identifier("one00-web")
            .parentAsset(managedWebspaceHostingAsset)
            .build();

    private final HsHostingAssetRealEntity anotherManagedWebspaceHostingAsset = HsHostingAssetRealEntity.builder()
            .uuid(UUID.randomUUID())
            .type(MANAGED_WEBSPACE)
            .identifier("two00")
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

    @BeforeEach
    void initMocks() {
        emwFake.persist(managedWebspaceHostingAsset);
        emwFake.persist(unixUserHostingAsset);
    }

    @Test
    void doesNotPersistEventEntityWithoutValidationErrors() {
        // given
        final var givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org"),
                entry("verificationCode", "just-a-fake-verification-code")
        );
        final var givenAssetJson = """
                {
                    "identifier": "example.org", // also as default for all subAssets
                    "subHostingAssets": [
                        {
                            "type": "DOMAIN_HTTP_SETUP",
                            "assignedToAsset.uuid": "{unixUserHostingAssetUuid}"
                        },
                        {
                            "type": "DOMAIN_DNS_SETUP"
                        },
                        {
                            "type": "DOMAIN_MBOX_SETUP"
                        },
                        {
                            "type": "DOMAIN_SMTP_SETUP"
                        }
                    ]
                }
                """
                .replace("{unixUserHostingAssetUuid}", unixUserHostingAsset.getUuid().toString());
        Dns.fakeResultForDomain("example.org",
                Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=just-a-fake-verification-code"));

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)
        );

        // then
        assertThat(emwFake.stream(BookingItemCreatedEventEntity.class))
                .as("the event should not have been persisted, but got persisted")
                .isEmpty();
    }

    @Test
    void persistsEventEntityIfDomainSetupVerificationFails() {
        // given
        final var givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org")
        );
        final var givenAssetJson = """
                {
                    "identifier": "example.org", // also as default for all subAssets
                    "subHostingAssets": [
                        {
                            "type": "DOMAIN_HTTP_SETUP",
                            "assignedToAsset.uuid": "{unixUserHostingAssetUuid}"
                        },
                        {
                            "type": "DOMAIN_DNS_SETUP"
                        },
                        {
                            "type": "DOMAIN_MBOX_SETUP"
                        },
                        {
                            "type": "DOMAIN_SMTP_SETUP"
                        }
                    ]
                }
                """
                .replace("{unixUserHostingAssetUuid}", unixUserHostingAsset.getUuid().toString());
        Dns.fakeResultForDomain("example.org", Dns.Result.fromRecords()); // without valid verificationCode

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)
        );

        // then
        assertEventStatus(givenBookingItem, givenAssetJson,
                "[[DNS] no TXT record 'Hostsharing-domain-setup-verification-code=null' found for domain name 'example.org' (nor in its super-domain)]");
    }

    @Test
    void persistsEventEntityIfDomainDnsSetupIsSupplied() {
        // given
        final var givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org"),
                entry("verificationCode", "just-a-fake-verification-code")
        );
        final var givenAssetJson = """
            {
                "identifier": "example.org", // also as default for all subAssets
                "subHostingAssets": [
                    {
                        "type": "DOMAIN_HTTP_SETUP",
                        "assignedToAsset.uuid": "{unixUserHostingAssetUuid}"
                    },
                    {
                        "type": "DOMAIN_DNS_SETUP"
                    },
                    {
                        "type": "DOMAIN_MBOX_SETUP"
                    },
                    {
                        "type": "DOMAIN_SMTP_SETUP"
                    }
                ]
            }
            """
                .replace("{unixUserHostingAssetUuid}", unixUserHostingAsset.getUuid().toString())
                .replace("{managedWebspaceHostingAssetUuid}", managedWebspaceHostingAsset.getUuid().toString());
        Dns.fakeResultForDomain("example.org",
                Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=just-a-fake-verification-code"));

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)
        );

        // then
        assertEventStatus(givenBookingItem, givenAssetJson,
                "domain DNS setup not allowed for legacy compatibility");
    }

    @Test
    void persistsEventEntityIfSuppliedDomainUnixUserAndSmtpSetupWebspaceDontMatch() {
        // given
        final var givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org"),
                entry("verificationCode", "just-a-fake-verification-code")
        );
        final var givenAssetJson = """
            {
                "identifier": "example.org", // also as default for all subAssets
                "subHostingAssets": [
                    {
                        "type": "DOMAIN_HTTP_SETUP",
                        "assignedToAsset.uuid": "{unixUserHostingAssetUuid}"
                    },
                    {
                        "type": "DOMAIN_DNS_SETUP"
                    },
                    {
                        "type": "DOMAIN_MBOX_SETUP"
                    },
                    {
                        "type": "DOMAIN_SMTP_SETUP"
                    }
                ]
            }
            """
                .replace("{unixUserHostingAssetUuid}", unixUserHostingAsset.getUuid().toString())
                .replace("{managedWebspaceHostingAssetUuid}", anotherManagedWebspaceHostingAsset.getUuid().toString());
        Dns.fakeResultForDomain("example.org",
                Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=just-a-fake-verification-code"));

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)
        );

        // then
        assertEventStatus(givenBookingItem, givenAssetJson,
                "domain DNS setup not allowed for legacy compatibility");
    }

    @SafeVarargs
    private static HsBookingItemRealEntity createBookingItemFromResources(final Map.Entry<String, String>... givenResources) {
        return HsBookingItemRealEntity.builder()
                .type(HsBookingItemType.DOMAIN_SETUP)
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
