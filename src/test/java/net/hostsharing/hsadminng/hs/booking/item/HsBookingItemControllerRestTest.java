package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemResource;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsBookingItemController.class)
@Import({StrictMapper.class, JsonObjectMapperConfiguration.class, DisableSecurityConfig.class, MessageTranslator.class})
@ActiveProfiles("test")
class HsBookingItemControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @Autowired
    @SuppressWarnings("unused") // not used in test, but in controller class
    StrictMapper mapper;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManagerFactory emf;

    @MockitoBean
    HsBookingProjectRealRepository realProjectRepo;

    @MockitoBean
    HsBookingItemRbacRepository rbacBookingItemRepo;

    @TestConfiguration
    public static class TestConfig {

        @Bean
        public EntityManager entityManager() {
            return mock(EntityManager.class);
        }

    }

    @BeforeEach
    void init() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
    }

    @Nested
    class PostNewBookingItem {

        @Test
        void globalAdmin_canAddValidBookingItem() throws Exception {

            final var givenProjectUuid = UUID.randomUUID();

            // given
            when(em.find(HsBookingProjectRealEntity.class, givenProjectUuid)).thenAnswer(invocation ->
                    HsBookingProjectRealEntity.builder()
                            .uuid(invocation.getArgument(1))
                            .build()
            );
            when(rbacBookingItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/booking/items")
                            .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "project.uuid": "{projectUuid}",
                                        "type": "MANAGED_SERVER",
                                        "caption": "some new booking",
                                        "validTo": "{validTo}",
                                        "resources": { "CPU": 12, "RAM": 4, "SSD": 100, "Traffic": 250 }
                                    }
                                    """
                                    .replace("{projectUuid}", givenProjectUuid.toString())
                                    .replace("{validTo}", LocalDate.now().plusMonths(1).toString())
                            )
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())

                    // then
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath(
                            "$", lenientlyEquals("""
                                    {
                                        "type": "MANAGED_SERVER",
                                        "caption": "some new booking",
                                        "validFrom": "{today}",
                                        "validTo": "{todayPlus1Month}",
                                        "resources": { "CPU": 12, "SSD": 100, "Traffic": 250 }
                                     }
                                    """
                                    .replace("{today}", LocalDate.now().toString())
                                    .replace("{todayPlus1Month}", LocalDate.now().plusMonths(1).toString()))
                    ))
                    .andExpect(header().string("Location", matchesRegex("http://localhost/api/hs/booking/items/[^/]*")));
        }

        @Test
        void globalAdmin_canNotAddInvalidBookingItem() throws Exception {

            final var givenProjectUuid = UUID.randomUUID();

            // given
            when(em.find(HsBookingProjectRealEntity.class, givenProjectUuid)).thenAnswer(invocation ->
                    HsBookingProjectRealEntity.builder()
                            .uuid(invocation.getArgument(1))
                            .build()
            );
            when(rbacBookingItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/booking/items")
                            .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "project.uuid": "{projectUuid}",
                                        "type": "MANAGED_SERVER",
                                        "caption": "some new booking",
                                        "validFrom": "{validFrom}", // not specified => not accepted
                                        "resources": { "CPU": 12, "RAM": 4, "SSD": 100, "Traffic": 250 }
                                    }
                                    """
                                    .replace("{projectUuid}", givenProjectUuid.toString())
                                    .replace("{validFrom}", LocalDate.now().plusMonths(1).toString())
                            )
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())

                    // then
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath(
                            "$", lenientlyEquals("""
                                    {
                                        "message": "ERROR: [400] JSON parse error: Unrecognized field \\"validFrom\\" (class ${resourceClass}), not marked as ignorable"
                                    }
                                    """.replace("${resourceClass}", HsBookingItemInsertResource.class.getName()))));
        }
    }

    @Nested
    class itemToResourcePostmapper {

        @Test
        void canConvertEmptyValidity() {

            // given
            final var givenProject = HsBookingProjectRealEntity.builder()
                    .uuid(UUID.randomUUID())
                    .build();
            when(em.find(HsBookingProjectRealEntity.class, givenProject.getUuid())).thenAnswer(invocation ->
                    HsBookingProjectRealEntity.builder()
                            .uuid(invocation.getArgument(1))
                            .build()
            );
            final var givenBookingItem = HsBookingItemRbacEntity.builder()
                    .uuid(UUID.randomUUID())
                    .project(givenProject)
                    .validity(Range.emptyRange(LocalDate.class))
                    .build();
            final var givenBookingResource = new HsBookingItemResource();

            // when
            HsBookingItemController.ITEM_TO_RESOURCE_POSTMAPPER.accept(
                    givenBookingItem, givenBookingResource);

            // then
            assertThat(givenBookingResource.getValidFrom()).isNull();
            assertThat(givenBookingResource.getValidTo()).isNull();
        }
    }
}
