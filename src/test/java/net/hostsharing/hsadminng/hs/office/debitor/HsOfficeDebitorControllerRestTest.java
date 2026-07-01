package net.hostsharing.hsadminng.hs.office.debitor;

import lombok.val;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeDebitorController.class)
@Import({ StrictMapper.class,
          JsonObjectMapperConfiguration.class,
          MessagesResourceConfig.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsOfficeDebitorControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    HsOfficeDebitorController controller;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManagerFactory emf;

    @MockitoBean
    HsOfficeDebitorRepository debitorRepo;

    @MockitoBean
    HsOfficeRelationRealRepository realRelRepo;

    @MockitoBean
    HsOfficePersonRealRepository realPersonRepo;

    @MockitoBean
    HsOfficeContactRealRepository realContactRepo;

    @MockitoBean
    HsOfficeBankAccountRepository bankAccountRepo;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(controller, "em", em);
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
    }

    @Nested
    class GetListOfDebitors {

        @Test
        void returnsDebitorsByPartnerNumber() throws Exception {
            // given
            when(debitorRepo.findDebitorsByPartnerNumber(12345))
                    .thenReturn(List.of(givenDebitor(UUID.randomUUID())));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/debitors?partnerNumber=P-12345")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].debitorNumber", is("D-1234500")))
                    .andExpect(jsonPath("$[0].partner.partnerNumber", is("P-12345")));
        }

        @Test
        void returnsDebitorsByPartnerUuid() throws Exception {
            // given
            val partnerUuid = UUID.randomUUID();
            when(debitorRepo.findDebitorsByPartnerUuid(partnerUuid))
                    .thenReturn(List.of(givenDebitor(UUID.randomUUID())));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/debitors?partnerUuid=" + partnerUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    class GetSingleDebitor {

        @Test
        void returnsDebitorByUuidIfFound() throws Exception {
            // given
            val debitorUuid = UUID.randomUUID();
            when(debitorRepo.findByUuid(debitorUuid)).thenReturn(Optional.of(givenDebitor(debitorUuid)));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/debitors/" + debitorUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("uuid", is(debitorUuid.toString())))
                    .andExpect(jsonPath("debitorNumber", is("D-1234500")));
        }

        @Test
        void returnsDebitorByNumberIfFound() throws Exception {
            // given
            when(debitorRepo.findDebitorByDebitorNumber(1234500)).thenReturn(Optional.of(givenDebitor(UUID.randomUUID())));

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/debitors/D-1234500")
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("debitorNumber", is("D-1234500")));
        }

        @Test
        void returnsNotFoundIfMissing() throws Exception {
            // given
            val debitorUuid = UUID.randomUUID();
            when(debitorRepo.findByUuid(debitorUuid)).thenReturn(Optional.empty());

            // when
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/hs/office/debitors/" + debitorUuid)
                            .header("Authorization", bearer("hsh-alex_superuser"))
                            .accept(MediaType.APPLICATION_JSON))

                    // then
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void rejectsPostWithoutDebitorRel() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/debitors")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "debitorNumberSuffix": "00",
                                    "billable": true,
                                    "defaultPrefix": "abc"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString("exactly one of debitorRel and debitorRelUuid must be supplied")));
    }

    @Test
    void postsNewDebitorWithExistingDebitorRel() throws Exception {
        // given
        val debitorRelUuid = UUID.randomUUID();
        val savedDebitorUuid = UUID.randomUUID();
        val debitorRel = givenRelation(debitorRelUuid);
        when(realRelRepo.findByUuid(debitorRelUuid)).thenReturn(Optional.of(debitorRel));
        when(realRelRepo.save(debitorRel)).thenReturn(debitorRel);
        when(debitorRepo.save(any(HsOfficeDebitorEntity.class))).thenAnswer(invocation -> {
            final HsOfficeDebitorEntity debitor = invocation.getArgument(0);
            debitor.setUuid(savedDebitorUuid);
            debitor.setPartner(givenPartner());
            return debitor;
        });

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/debitors")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "debitorRel.uuid": "%s",
                                    "debitorNumberSuffix": "00",
                                    "billable": true,
                                    "defaultPrefix": "abc"
                                }
                                """.formatted(debitorRelUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("uuid", is(savedDebitorUuid.toString())))
                .andExpect(jsonPath("debitorNumber", is("D-1234500")));
    }

    @Test
    void deletesDebitor() throws Exception {
        // given
        val debitorUuid = UUID.randomUUID();
        when(debitorRepo.deleteByUuid(debitorUuid)).thenReturn(1);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/hs/office/debitors/" + debitorUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNoContent());
    }

    @Test
    void patchesDebitor() throws Exception {
        // given
        val debitorUuid = UUID.randomUUID();
        val current = givenDebitor(debitorUuid);
        when(debitorRepo.findByUuid(debitorUuid)).thenReturn(Optional.of(current));
        when(debitorRepo.save(any(HsOfficeDebitorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/hs/office/debitors/" + debitorUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "billable": false,
                                    "vatId": "VAT123"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("billable", is(false)))
                .andExpect(jsonPath("vatId", is("VAT123")));
    }

    private HsOfficeDebitorEntity givenDebitor(final UUID uuid) {
        return HsOfficeDebitorEntity.builder()
                .uuid(uuid)
                .partner(givenPartner())
                .debitorRel(givenRelation(UUID.randomUUID()))
                .debitorNumberSuffix("00")
                .billable(true)
                .defaultPrefix("abc")
                .build();
    }

    private HsOfficePartnerRealEntity givenPartner() {
        return HsOfficePartnerRealEntity.builder()
                .partnerNumber(12345)
                .partnerRel(givenRelation(UUID.randomUUID()))
                .build();
    }

    private HsOfficeRelationRealEntity givenRelation(final UUID uuid) {
        val person = HsOfficePersonRealEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(HsOfficePersonType.LEGAL_PERSON)
                .tradeName("Partner GmbH")
                .build();
        return HsOfficeRelationRealEntity.builder()
                .uuid(uuid)
                .type(HsOfficeRelationType.DEBITOR)
                .anchor(person)
                .holder(person)
                .contact(HsOfficeContactRealEntity.builder()
                        .uuid(UUID.randomUUID())
                        .caption("Debitor contact")
                        .build())
                .build();
    }
}
