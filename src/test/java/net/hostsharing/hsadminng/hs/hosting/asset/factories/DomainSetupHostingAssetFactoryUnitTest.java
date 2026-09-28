package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import lombok.val;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedAppEvent;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedEventEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacRepository;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.Dns;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapperFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import jakarta.validation.ValidationException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static net.hostsharing.hsadminng.mapper.PatchableMapWrapper.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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

    private EntityManagerWrapperFake emwFake = new EntityManagerWrapperFake();

    @Spy
    private EntityManagerWrapper emw = emwFake;

    @Spy
    private ObjectMapper jsonMapper = new JsonObjectMapperConfiguration().customObjectMapper();

    @Spy
    private StrictMapper StrictMapper = new StrictMapper(emw);

    @Mock
    private HsOfficeContactRbacRepository rbacContactRepo;

    @Mock
    private HsHostingAssetRbacRepository rbacAssetRepo;

    @InjectMocks
    private HsBookingItemCreatedListener listener;

    @BeforeEach
    void initMocks() {
        emwFake.persist(managedWebspaceHostingAsset);
        emwFake.persist(unixUserHostingAsset);
        lenient().when(rbacAssetRepo.findVisibleByUuid(unixUserHostingAsset.getUuid()))
                .thenReturn(Optional.of(unixUserHostingAsset));
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
    void rejectsOverSpecifiedSubHostingAsset() {
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
                        "type": "DOMAIN_DNS_SETUP",
                        "caption": "own DNS setup"
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
        val thrown = catchThrowable(() -> listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)));

        // then
        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("sub asset DOMAIN_DNS_SETUP is over-specified, in compatibility mode, only default values allowed");
        assertNoEventPersisted();
    }

    // verifies Fix for Security-Finding A-02
    @Test
    void persistsEventEntityIfAlarmContactIsNotVisible() {
        // given
        val invisibleAlarmContact = HsOfficeContactRealEntity.builder()
                .uuid(UUID.randomUUID())
                .caption("some contact of a foreign customer")
                .build();
        emwFake.persist(invisibleAlarmContact); // exists in the raw table, but rbacContactRepo does not find it
        final var givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org"),
                entry("verificationCode", "just-a-fake-verification-code")
        );
        final var givenAssetJson = """
                {
                    "identifier": "example.org", // also as default for all subAssets
                    "alarmContact.uuid": "{alarmContactUuid}",
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
                .replace("{alarmContactUuid}", invisibleAlarmContact.getUuid().toString())
                .replace("{unixUserHostingAssetUuid}", unixUserHostingAsset.getUuid().toString());
        Dns.fakeResultForDomain("example.org",
                Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=just-a-fake-verification-code"));

        // when
        listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)
        );

        // then
        assertEventStatus(givenBookingItem, givenAssetJson,
                "cannot find Contact by alarmContact.uuid: " + invisibleAlarmContact.getUuid());
        assertNoDomainSetupAssetPersisted();
    }

    // verifies Fix for Security-Finding A-02
    @Test
    void persistsEventEntityIfAssignedToAssetIsNotVisible() {
        // given
        when(rbacAssetRepo.findVisibleByUuid(unixUserHostingAsset.getUuid()))
                .thenReturn(Optional.empty()); // exists in the raw table (see initMocks), but is not visible
        val givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org"),
                entry("verificationCode", "just-a-fake-verification-code")
        );
        val givenAssetJson = """
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
        assertEventStatus(givenBookingItem, givenAssetJson,
                "DOMAIN_HTTP_SETUP subAsset's assignedToAsset.uuid not found");
        assertNoDomainSetupAssetPersisted();
    }

    @Test
    void rejectsIncompleteSubHostingAssets() {
        // given
        val givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org"),
                entry("verificationCode", "just-a-fake-verification-code")
        );
        val givenAssetJson = """
                {
                    "identifier": "example.org", // also as default for all subAssets
                    "subHostingAssets": [
                        {
                            "type": "DOMAIN_HTTP_SETUP",
                            "assignedToAsset.uuid": "{unixUserHostingAssetUuid}"
                        },
                        {
                            "type": "DOMAIN_DNS_SETUP"
                        }
                    ]
                }
                """
                .replace("{unixUserHostingAssetUuid}", unixUserHostingAsset.getUuid().toString());

        // when
        val thrown = catchThrowable(() -> listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)));

        // then
        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("sub-asset of type DOMAIN_MBOX_SETUP required in legacy mode, but missing");
        assertNoEventPersisted();
    }

    @Test
    void rejectsDomainHttpSetupWithoutAssignedToAsset() {
        // given
        val givenBookingItem = createBookingItemFromResources(
                entry("domainName", "example.org"),
                entry("verificationCode", "just-a-fake-verification-code")
        );
        val givenAssetJson = """
                {
                    "identifier": "example.org", // also as default for all subAssets
                    "subHostingAssets": [
                        {
                            "type": "DOMAIN_HTTP_SETUP"
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
                """;

        // when
        val thrown = catchThrowable(() -> listener.onApplicationEvent(
                new BookingItemCreatedAppEvent(this, givenBookingItem, givenAssetJson)));

        // then
        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("DOMAIN_HTTP_SETUP subAsset with assignedToAsset.uuid required in compatibility mode");
        assertNoEventPersisted();
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
        val eventEntity = emwFake.stream(BookingItemCreatedEventEntity.class)
                .reduce(Reducer::toSingleElement)
                .orElseThrow(() -> new AssertionError("the event should have been persisted, but got not persisted"));
        assertThat(eventEntity.getBookingItem()).isSameAs(givenBookingItem);
        assertThat(eventEntity.getAssetJson()).isEqualTo(givenAssetJson);
        assertThat(eventEntity.getStatusMessage()).isEqualTo(expectedErrorMessage);
    }

    private void assertNoEventPersisted() {
        assertThat(emwFake.stream(BookingItemCreatedEventEntity.class))
                .as("the event should not have been persisted, but got persisted")
                .isEmpty();
    }

    private void assertNoDomainSetupAssetPersisted() {
        assertThat(emwFake.stream(HsHostingAssetRealEntity.class))
                .noneMatch(asset -> "example.org".equals(asset.getIdentifier()));
    }
}
