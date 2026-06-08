package net.hostsharing.hsadminng.hs.booking.item;

import lombok.val;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemResource;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapperFake;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapperFakeConfiguration;
import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsBookingItemController.class)
@Import({StrictMapper.class,
         JsonObjectMapperConfiguration.class,
         MessageTranslator.class,
         EntityManagerWrapperFakeConfiguration.class,
         WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsBookingItemControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @Autowired
    @SuppressWarnings("unused") // not used in test, but in controller class
    StrictMapper mapper;

    @Autowired
    EntityManagerWrapperFake em;

    @MockitoBean
    EntityManagerFactory emf;

    @MockitoBean
    HsBookingProjectRealRepository realProjectRepo;

    @MockitoBean
    HsBookingItemRbacRepository rbacBookingItemRepo;

    @BeforeEach
    void init() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
        em.clear();
    }

    @Nested
    class GetListOfBookingItems {

        @Test
        void returnsBookingItemsByProjectUuid() throws Exception {
            // given
            val projectUuid = UUID.randomUUID();
            when(rbacBookingItemRepo.findAllByProjectUuid(projectUuid))
                    .thenReturn(List.of(givenManagedServerBookingItem(UUID.randomUUID(), "some booking")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/booking/items?projectUuid=" + projectUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].caption", is("some booking")))
                    .andExpect(jsonPath("$[0].type", is("MANAGED_SERVER")));
        }
    }

    @Nested
    class GetSingleBookingItem {

        @Test
        void returnsBookingItemIfFound() throws Exception {
            // given
            val bookingItemUuid = UUID.randomUUID();
            when(rbacBookingItemRepo.findByUuid(bookingItemUuid))
                    .thenReturn(Optional.of(givenManagedServerBookingItem(bookingItemUuid, "single booking")));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/booking/items/" + bookingItemUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(bookingItemUuid.toString())))
                    .andExpect(jsonPath("caption", is("single booking")))
                    .andExpect(jsonPath("resources.CPU", is(2)));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val bookingItemUuid = UUID.randomUUID();
            when(rbacBookingItemRepo.findByUuid(bookingItemUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/booking/items/" + bookingItemUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class PostNewBookingItem {

        @Test
        void globalAdmin_canAddValidBookingItem() throws Exception {

            val givenProjectUuid = UUID.randomUUID();

            // given
            em.persist(HsBookingProjectRealEntity.builder()
                    .uuid(givenProjectUuid)
                    .build());
            when(rbacBookingItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/booking/items")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
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

            val givenProjectUuid = UUID.randomUUID();

            // given
            em.persist(HsBookingProjectRealEntity.builder()
                    .uuid(givenProjectUuid)
                    .build());
            when(rbacBookingItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/hs/booking/items")
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
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
    class DeleteBookingItem {

        @Test
        void respondsNoContentIfDeleted() throws Exception {
            // given
            val bookingItemUuid = UUID.randomUUID();
            when(rbacBookingItemRepo.deleteByUuid(bookingItemUuid)).thenReturn(1);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/booking/items/" + bookingItemUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNoContent());
        }

        @Test
        void respondsNotFoundIfMissing() throws Exception {
            // given
            val bookingItemUuid = UUID.randomUUID();
            when(rbacBookingItemRepo.deleteByUuid(bookingItemUuid)).thenReturn(0);

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .delete("/api/hs/booking/items/" + bookingItemUuid)
                            .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void patchesBookingItem() throws Exception {
        // given
        val bookingItemUuid = UUID.randomUUID();
        when(rbacBookingItemRepo.findByUuid(bookingItemUuid))
                .thenReturn(Optional.of(givenManagedServerBookingItem(bookingItemUuid, "old booking")));
        when(rbacBookingItemRepo.save(any(HsBookingItemRbacEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/hs/booking/items/" + bookingItemUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "caption": "patched booking",
                                    "validTo": "%s",
                                    "resources": {
                                        "CPU": 4,
                                        "RAM": 8,
                                        "SSD": 100,
                                        "Traffic": 500
                                    }
                                }
                                """.formatted(LocalDate.now().plusMonths(2)))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid", is(bookingItemUuid.toString())))
                .andExpect(jsonPath("caption", is("patched booking")))
                .andExpect(jsonPath("resources.CPU", is(4)));
    }

    @Nested
    class itemToResourcePostmapper {

        @Test
        void canConvertEmptyValidity() {

            // given
            val givenProject = HsBookingProjectRealEntity.builder()
                    .uuid(UUID.randomUUID())
                    .build();
            val givenBookingItem = HsBookingItemRbacEntity.builder()
                    .uuid(UUID.randomUUID())
                    .project(givenProject)
                    .validity(Range.emptyRange(LocalDate.class))
                    .build();
            val givenBookingResource = new HsBookingItemResource();

            // when
            HsBookingItemController.ITEM_TO_RESOURCE_POSTMAPPER.accept(
                    givenBookingItem, givenBookingResource);

            // then
            assertThat(givenBookingResource.getValidFrom()).isNull();
            assertThat(givenBookingResource.getValidTo()).isNull();
        }
    }

    private HsBookingItemRbacEntity givenManagedServerBookingItem(final UUID bookingItemUuid, final String caption) {
        return HsBookingItemRbacEntity.builder()
                .uuid(bookingItemUuid)
                .project(HsBookingProjectRealEntity.builder()
                        .uuid(UUID.randomUUID())
                        .build())
                .type(HsBookingItemType.MANAGED_SERVER)
                .caption(caption)
                .validity(Range.closedOpen(LocalDate.now(), LocalDate.now().plusMonths(1)))
                .resources(new HashMap<>(Map.of(
                        "CPU", 2,
                        "RAM", 4,
                        "SSD", 50,
                        "Traffic", 250)))
                .build();
    }
}
