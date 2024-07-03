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
