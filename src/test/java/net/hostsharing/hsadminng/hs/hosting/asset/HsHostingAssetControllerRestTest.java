package net.hostsharing.hsadminng.hs.hosting.asset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRepository;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_CLOUD_SERVER_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_MANAGED_SERVER_BOOKING_ITEM;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_SERVER_HOSTING_ASSET;
import static net.hostsharing.hsadminng.hs.hosting.asset.TestHsHostingAssetEntities.TEST_MANAGED_WEBSPACE_HOSTING_ASSET;
import static net.hostsharing.hsadminng.hs.office.contact.TestHsOfficeContact.TEST_CONTACT;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsHostingAssetController.class)
@Import(Mapper.class)
@RunWith(SpringRunner.class)
public class HsHostingAssetControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Context contextMock;

    @Autowired
    Mapper mapper;

    @Mock
    private EntityManager em;

    @MockBean
    EntityManagerFactory emf;

    @MockBean
    @SuppressWarnings("unused") // bean needs to be present for HsHostingAssetController
    private HsBookingItemRepository bookingItemRepo;

    @MockBean
    private HsHostingAssetRepository hostingAssetRepo;

    enum ListTestCases {
        CLOUD_SERVER(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.CLOUD_SERVER)
                                .bookingItem(TEST_CLOUD_SERVER_BOOKING_ITEM)
                                .identifier("vm1234")
                                .caption("some fake cloud-server")
                                .alarmContact(TEST_CONTACT)
                                .build()),
                """
                    [
                        {
                            "type": "CLOUD_SERVER",
                            "identifier": "vm1234",
                            "caption": "some fake cloud-server",
                            "alarmContact": {
                                "caption": "some contact",
                                "postalAddress": "address of some contact",
                                "emailAddresses": {
                                    "main": "some-contact@example.com"
                                }
                            },
                            "config": {}
                        }
                    ]
                """),
        MANAGED_SERVER(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.MANAGED_SERVER)
                                .bookingItem(TEST_MANAGED_SERVER_BOOKING_ITEM)
                                .identifier("vm1234")
                                .caption("some fake managed-server")
                                .alarmContact(TEST_CONTACT)
                                .config(Map.ofEntries(
                                        entry("monit_max_ssd_usage", 70),
                                        entry("monit_max_cpu_usage", 80),
                                        entry("monit_max_ram_usage", 90)
                                ))
                                .build()),
                """
                    [
                        {
                            "type": "MANAGED_SERVER",
                            "identifier": "vm1234",
                            "caption": "some fake managed-server",
                            "alarmContact": {
                                "caption": "some contact",
                                "postalAddress": "address of some contact",
                                "emailAddresses": {
                                    "main": "some-contact@example.com"
                                }
                            },
                            "config": {
                                "monit_max_ssd_usage": 70,
                                "monit_max_cpu_usage": 80,
                                "monit_max_ram_usage": 90
                            }
                        }
                    ]
                """),
        UNIX_USER(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.UNIX_USER)
                                .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
                                .identifier("xyz00-office")
                                .caption("some fake Unix-User")
                                .config(Map.ofEntries(
                                        entry("password", "$6$salt$hashed-salted-password"),
                                        entry("totpKey", "0x0123456789abcdef"),
                                        entry("shell", "/bin/bash"),
                                        entry("SSD-soft-quota", 128),
                                        entry("SSD-hard-quota", 256),
                                        entry("HDD-soft-quota", 256),
                                        entry("HDD-hard-quota", 512)))
                                .build()),
                """
                    [
                        {
                            "type": "UNIX_USER",
                            "identifier": "xyz00-office",
                            "caption": "some fake Unix-User",
                            "alarmContact": null,
                            "config": {
                                "SSD-soft-quota": 128,
                                "SSD-hard-quota": 256,
                                "HDD-soft-quota": 256,
                                "HDD-hard-quota": 512,
                                "shell": "/bin/bash",
                                "homedir": "/home/pacs/xyz00/users/office"
                            }
                        }
                    ]
                """),
        EMAIL_ALIAS(
                List.of(
                    HsHostingAssetEntity.builder()
                            .type(HsHostingAssetType.EMAIL_ALIAS)
                            .parentAsset(TEST_MANAGED_WEBSPACE_HOSTING_ASSET)
                            .identifier("xyz00-office")
                            .caption("some fake EMail-Alias")
                            .config(Map.ofEntries(
                                    entry("target", Array.of("xyz00", "xyz00-abc", "office@example.com"))
                            ))
                            .build()),
                """
                    [
                        {
                            "type": "EMAIL_ALIAS",
                            "identifier": "xyz00-office",
                            "caption": "some fake EMail-Alias",
                            "alarmContact": null,
                            "config": {
                                "target": ["xyz00","xyz00-abc","office@example.com"]
                            }
                        }
                    ]
                """),
        DOMAIN_SETUP(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.DOMAIN_SETUP)
                                .identifier("example.org")
                                .caption("some fake Domain-Setup")
                                .build()),
                """
                    [
                        {
                            "type": "DOMAIN_SETUP",
                            "identifier": "example.org",
                            "caption": "some fake Domain-Setup",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """),
        DOMAIN_DNS_SETUP(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.DOMAIN_DNS_SETUP)
                                .identifier("example.org")
                                .caption("some fake Domain-DNS-Setup")
                                .config(Map.ofEntries(
                                        entry("auto-WILDCARD-MX-RR", false),
                                        entry("auto-WILDCARD-A-RR", false),
                                        entry("auto-WILDCARD-AAAA-RR", false),
                                        entry("auto-WILDCARD-DKIM-RR", false),
                                        entry("auto-WILDCARD-SPF-RR", false),
                                        entry("user-RR", Array.of(
                                                "www            IN          CNAME example.com. ; www.example.com is an alias for example.com",
                                                "test1          IN 1h30m    CNAME example.com.",
                                                "test2 1h30m    IN          CNAME example.com.",
                                                "ns             IN          A     192.0.2.2; IPv4 address for ns.example.com")
                                        )
                                ))
                                .build()),
                """
                    [
                        {
                            "type": "DOMAIN_DNS_SETUP",
                            "identifier": "example.org",
                            "caption": "some fake Domain-DNS-Setup",
                            "alarmContact": null,
                            "config": {
                                "auto-WILDCARD-AAAA-RR": false,
                                "auto-WILDCARD-MX-RR": false,
                                "auto-WILDCARD-SPF-RR": false,
                                "auto-WILDCARD-DKIM-RR": false,
                                "auto-WILDCARD-A-RR": false,
                                "user-RR": [
                                    "www            IN          CNAME example.com. ; www.example.com is an alias for example.com",
                                    "test1          IN 1h30m    CNAME example.com.",
                                    "test2 1h30m    IN          CNAME example.com.",
                                    "ns             IN          A     192.0.2.2; IPv4 address for ns.example.com"
                                ]
                            }
                        }
                    ]
                """),
        DOMAIN_HTTP_SETUP(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.DOMAIN_HTTP_SETUP)
                                .identifier("example.org|HTTP")
                                .caption("some fake Domain-HTTP-Setup")
                                .config(Map.ofEntries(
                                        entry("htdocsfallback", false),
                                        entry("indexes", false),
                                        entry("cgi", false),
                                        entry("passenger", false),
                                        entry("passenger-errorpage", true),
                                        entry("fastcgi", false),
                                        entry("autoconfig", false),
                                        entry("greylisting", false),
                                        entry("includes", false),
                                        entry("letsencrypt", false),
                                        entry("multiviews", false),
                                        entry("fcgi-php-bin", "/usr/lib/cgi-bin/php8"),
                                        entry("passenger-nodejs", "/usr/bin/node-js7"),
                                        entry("passenger-python", "/usr/bin/python6"),
                                        entry("passenger-ruby", "/usr/bin/ruby5"),
                                        entry("subdomains", Array.of("www", "test1", "test2"))
                                ))
                                .build()),
                """
                    [
                        {
                            "type": "DOMAIN_HTTP_SETUP",
                            "identifier": "example.org|HTTP",
                            "caption": "some fake Domain-HTTP-Setup",
                            "alarmContact": null,
                            "config": {
                                "autoconfig": false,
                                "cgi": false,
                                "fastcgi": false,
                                "greylisting": false,
                                "htdocsfallback": false,
                                "includes": false,
                                "indexes": false,
                                "letsencrypt": false,
                                "multiviews": false,
                                "passenger": false,
                                "passenger-errorpage": true,
                                "passenger-nodejs": "/usr/bin/node-js7",
                                "passenger-python": "/usr/bin/python6",
                                "passenger-ruby": "/usr/bin/ruby5",
                                "fcgi-php-bin": "/usr/lib/cgi-bin/php8",
                                "subdomains": ["www","test1","test2"]
                            }
                        }
                    ]
                """),
        DOMAIN_SMTP_SETUP(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.DOMAIN_SMTP_SETUP)
                                .identifier("example.org|SMTP")
                                .caption("some fake Domain-SMTP-Setup")
                                .build()),
                """
                    [
                        {
                            "type": "DOMAIN_SMTP_SETUP",
                            "identifier": "example.org|SMTP",
                            "caption": "some fake Domain-SMTP-Setup",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """),
        DOMAIN_MBOX_SETUP(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.DOMAIN_MBOX_SETUP)
                                .identifier("example.org|MBOX")
                                .caption("some fake Domain-MBOX-Setup")
                                .build()),
                """
                    [
                        {
                            "type": "DOMAIN_MBOX_SETUP",
                            "identifier": "example.org|MBOX",
                            "caption": "some fake Domain-MBOX-Setup",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """),
        EMAIL_ADDRESS(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.EMAIL_ADDRESS)
                                .parentAsset(HsHostingAssetEntity.builder()
                                        .type(HsHostingAssetType.DOMAIN_MBOX_SETUP)
                                        .identifier("example.org|MBOX")
                                        .caption("some fake Domain-MBOX-Setup")
                                        .build())
                                .identifier("office@example.org")
                                .caption("some fake EMail-Address")
                                .config(Map.ofEntries(
                                        entry("target", Array.of("xyz00", "xyz00-abc", "office@example.com"))
                                ))
                                .build()),
                """
                    [
                        {
                            "type": "EMAIL_ADDRESS",
                            "identifier": "office@example.org",
                            "caption": "some fake EMail-Address",
                            "alarmContact": null,
                            "config": {
                                "target": ["xyz00","xyz00-abc","office@example.com"]
                            }
                        }
                    ]
                """),
        MARIADB_INSTANCE(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.MARIADB_INSTANCE)
                                .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                                .identifier("vm1234|MariaDB.default")
                                .caption("some fake MariaDB instance")
                                .build()),
                """
                    [
                        {
                            "type": "MARIADB_INSTANCE",
                            "identifier": "vm1234|MariaDB.default",
                            "caption": "some fake MariaDB instance",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """),
        MARIADB_USER(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.MARIADB_USER)
                                .identifier("xyz00_temp")
                                .caption("some fake MariaDB user")
                                .build()),
                """
                    [
                        {
                            "type": "MARIADB_USER",
                            "identifier": "xyz00_temp",
                            "caption": "some fake MariaDB user",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """),
        MARIADB_DATABASE(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.MARIADB_DATABASE)
                                .identifier("xyz00_temp")
                                .caption("some fake MariaDB database")
                                .config(Map.ofEntries(
                                        entry("encoding", "latin1"),
                                        entry("collation", "latin2")
                                ))
                                .build()),
                """
                    [
                        {
                            "type": "MARIADB_DATABASE",
                            "identifier": "xyz00_temp",
                            "caption": "some fake MariaDB database",
                            "alarmContact": null,
                            "config": {
                                "encoding": "latin1",
                                "collation": "latin2"
                            }
                        }
                    ]
                """),
        PGSQL_INSTANCE(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.PGSQL_INSTANCE)
                                .parentAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                                .identifier("vm1234|PgSql.default")
                                .caption("some fake PgSql instance")
                                .build()),
                """
                    [
                        {
                            "type": "PGSQL_INSTANCE",
                            "identifier": "vm1234|PgSql.default",
                            "caption": "some fake PgSql instance",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """),
        PGSQL_USER(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.PGSQL_USER)
                                .identifier("xyz00_temp")
                                .caption("some fake PgSql user")
                                .build()),
                """
                    [
                        {
                            "type": "PGSQL_USER",
                            "identifier": "xyz00_temp",
                            "caption": "some fake PgSql user",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """),
        PGSQL_DATABASE(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.PGSQL_DATABASE)
                                .identifier("xyz00_temp")
                                .caption("some fake PgSql database")
                                .config(Map.ofEntries(
                                        entry("encoding", "latin1"),
                                        entry("collation", "latin2")
                                ))
                                .build()),
                """
                    [
                        {
                            "type": "PGSQL_DATABASE",
                            "identifier": "xyz00_temp",
                            "caption": "some fake PgSql database",
                            "alarmContact": null,
                            "config": {
                                "encoding": "latin1",
                                "collation": "latin2"
                            }
                        }
                    ]
                """),
        IPV4_NUMBER(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.IPV4_NUMBER)
                                .assignedToAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                                .identifier("11.12.13.14")
                                .caption("some fake IPv4 number")
                                .build()),
                """
                    [
                        {
                            "type": "IPV4_NUMBER",
                            "identifier": "11.12.13.14",
                            "caption": "some fake IPv4 number",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """),
        IPV6_NUMBER(
                List.of(
                        HsHostingAssetEntity.builder()
                                .type(HsHostingAssetType.IPV6_NUMBER)
                                .assignedToAsset(TEST_MANAGED_SERVER_HOSTING_ASSET)
                                .identifier("2001:db8:3333:4444:5555:6666:7777:8888")
                                .caption("some fake IPv6 number")
                                .build()),
                """
                    [
                        {
                            "type": "IPV6_NUMBER",
                            "identifier": "2001:db8:3333:4444:5555:6666:7777:8888",
                            "caption": "some fake IPv6 number",
                            "alarmContact": null,
                            "config": {}
                        }
                    ]
                """);

        final HsHostingAssetType assetType;
        final List<HsHostingAssetEntity> givenHostingAssetsOfType;
        final String expectedResponse;
        final JsonNode expectedResponseJson;

        @SneakyThrows
        ListTestCases(
                final List<HsHostingAssetEntity> givenHostingAssetsOfType,
                final String expectedResponse) {
            this.assetType = HsHostingAssetType.valueOf(name());
            this.givenHostingAssetsOfType = givenHostingAssetsOfType;
            this.expectedResponse = expectedResponse;
            this.expectedResponseJson =  new ObjectMapper().readTree(expectedResponse);
        }

        @SneakyThrows
        JsonNode expectedConfig(final int n) {
            return expectedResponseJson.get(n).path("config");
        }
    }

    @BeforeEach
    void init() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
    }

    @ParameterizedTest
    @EnumSource(HsHostingAssetControllerRestTest.ListTestCases.class)
    void shouldListAssets(final HsHostingAssetControllerRestTest.ListTestCases testCase) throws Exception {
        // given
        when(hostingAssetRepo.findAllByCriteria(null, null, testCase.assetType))
                .thenReturn(testCase.givenHostingAssetsOfType);

        // when
        final var result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/hosting/assets?type="+testCase.name())
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", lenientlyEquals(testCase.expectedResponse)))
                .andReturn();

        // and the config properties do match not just leniently but even strictly
        final var resultBody = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        for (int n = 0; n < resultBody.size(); ++n) {
            assertThat(resultBody.get(n).path("config")).isEqualTo(testCase.expectedConfig(n));
        }
    }
}
