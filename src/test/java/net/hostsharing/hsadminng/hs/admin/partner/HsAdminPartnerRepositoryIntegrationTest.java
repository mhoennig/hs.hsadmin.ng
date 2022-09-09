package net.hostsharing.hsadminng.hs.admin.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.admin.contact.HsAdminContactRepository;
import net.hostsharing.hsadminng.hs.admin.person.HsAdminPersonEntity;
import net.hostsharing.hsadminng.hs.admin.person.HsAdminPersonRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleRepository;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantDisplayExtractor.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleNameExtractor.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { HsAdminPartnerRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsAdminPartnerRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsAdminPartnerRepository partnerRepo;

    @Autowired
    HsAdminPersonRepository personRepo;

    @Autowired
    HsAdminContactRepository contactRepo;

    @Autowired
    RbacRoleRepository roleRepo;

    @Autowired
    RbacGrantRepository grantRepo;

    @Autowired
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockBean
    HttpServletRequest request;

    @Nested
    class CreatePartner {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewPartner() {
            // given
            context("alex@hostsharing.net");
            final var count = partnerRepo.count();
            final var givenPerson = personRepo.findPersonByOptionalNameLike("First Impressions GmbH").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("first contact").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newPartner = HsAdminPartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(givenPerson)
                        .contact(givenContact)
                        .build();
                return partnerRepo.save(newPartner);
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsAdminPartnerEntity::getUuid).isNotNull();
            assertThatPartnerIsPersisted(result.returnedValue());
            assertThat(partnerRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("alex@hostsharing.net");
            final var initialRoleCount = roleRepo.findAll().size();
            final var initialGrantCount = grantRepo.findAll().size();

            // when
            attempt(em, () -> {
                final var givenPerson = personRepo.findPersonByOptionalNameLike("Erbengemeinschaft Bessler").get(0);
                final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
                final var newPartner = HsAdminPartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(givenPerson)
                        .contact(givenContact)
                        .build();
                return partnerRepo.save(newPartner);
            });

            // then
            final var roles = roleRepo.findAll();
            assertThat(roleNamesOf(roles)).containsAll(List.of(
                    "hs_admin_partner#ErbengemeinschaftBesslerMelBessler-forthcontact.admin",
                    "hs_admin_partner#ErbengemeinschaftBesslerMelBessler-forthcontact.owner",
                    "hs_admin_partner#ErbengemeinschaftBesslerMelBessler-forthcontact.tenant"));
            assertThat(roles.size()).as("invalid number of roles created")
                    .isEqualTo(initialRoleCount + 3);

            context("customer-admin@forthcontact.example.com");
            assertThat(grantDisplaysOf(grantRepo.findAll())).containsAll(List.of(
                    "{ grant assumed role hs_admin_contact#forthcontact.owner to user customer-admin@forthcontact.example.com by role global#global.admin }"));

            context("person-ErbengemeinschaftBesslerMelBessl@example.com");
            assertThat(grantDisplaysOf(grantRepo.findAll())).containsAll(List.of(
                    "{ grant assumed role hs_admin_person#ErbengemeinschaftBesslerMelBessler.owner to user person-ErbengemeinschaftBesslerMelBessl@example.com by role global#global.admin }"));
        }

        private void assertThatPartnerIsPersisted(final HsAdminPartnerEntity saved) {
            final var found = partnerRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllPartners {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPartners() {
            // given
            context("alex@hostsharing.net");

            // when
            final var result = partnerRepo.findPartnerByOptionalNameLike(null);

            // then
            allThesePartnersAreReturned(result, "First Impressions GmbH", "Ostfriesische Kuhhandel OHG", "Rockshop e.K.");
        }

        @Test
        public void normalUser_canViewOnlyRelatedPartners() {
            // given:
            context("person-FirstImpressionsGmbH@example.com");

            // when:
            final var result = partnerRepo.findPartnerByOptionalNameLike(null);

            // then:
            exactlyThesePartnersAreReturned(result, "First Impressions GmbH");
        }
    }

    @Nested
    class FindByNameLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPartners() {
            // given
            context("alex@hostsharing.net");

            // when
            final var result = partnerRepo.findPartnerByOptionalNameLike("Ostfriesische");

            // then
            exactlyThesePartnersAreReturned(result, "Ostfriesische Kuhhandel OHG");
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyPartner() {
            // given
            context("alex@hostsharing.net", null);
            final var givenPartner = givenSomeTemporaryPartnerBessler();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net");
                partnerRepo.deleteByUuid(givenPartner.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("fran@hostsharing.net", null);
                return partnerRepo.findByUuid(givenPartner.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void nonGlobalAdmin_canNotDeleteTheirRelatedPartner() {
            // given
            context("alex@hostsharing.net", null);
            final var givenPartner = givenSomeTemporaryPartnerBessler();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-ErbengemeinschaftBesslerMelBessl@example.com");
                assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent();

                partnerRepo.deleteByUuid(givenPartner.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] User person-ErbengemeinschaftBesslerMelBessl@example.com not allowed to delete partner");
            assertThat(jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net");
                return partnerRepo.findByUuid(givenPartner.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingAPartnerAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("alex@hostsharing.net");
            final var initialRoleCount = roleRepo.findAll().size();
            final var givenPartner = givenSomeTemporaryPartnerBessler();
            assumeThat(roleRepo.findAll().size()).as("unexpected number of roles created")
                    .isEqualTo(initialRoleCount + 3);;

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net");
                partnerRepo.deleteByUuid(givenPartner.getUuid());
            }).assertSuccessful();

            // then
            final var roles = roleRepo.findAll();
            assertThat(roleNamesOf(roles)).doesNotContainAnyElementsOf(List.of(
                    "hs_admin_partner#ErbengemeinschaftBesslerMelBessler-forthcontact.admin",
                    "hs_admin_partner#ErbengemeinschaftBesslerMelBessler-forthcontact.owner",
                    "hs_admin_partner#ErbengemeinschaftBesslerMelBessler-forthcontact.tenant"));
            assertThat(roles.size()).as("invalid number of roles created")
                    .isEqualTo(initialRoleCount);

            context("customer-admin@forthcontact.example.com");
            assertThat(grantDisplaysOf(grantRepo.findAll())).doesNotContain(
                    "{ grant assumed role hs_admin_contact#forthcontact.owner to user customer-admin@forthcontact.example.com by role global#global.admin }");

            context("person-ErbengemeinschaftBesslerMelBessl@example.com");
            assertThat(grantDisplaysOf(grantRepo.findAll())).doesNotContain(
                    "{ grant assumed role hs_admin_person#ErbengemeinschaftBesslerMelBessler.owner to user person-ErbengemeinschaftBesslerMelBessl@example.com by role global#global.admin }");
        }
    }

    private HsAdminPartnerEntity givenSomeTemporaryPartnerBessler() {
        return jpaAttempt.transacted(() -> {
            context("alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Erbengemeinschaft Bessler").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
            final var newPartner = HsAdminPartnerEntity.builder()
                    .uuid(UUID.randomUUID())
                    .person(givenPerson)
                    .contact(givenContact)
                    .build();

            return partnerRepo.save(newPartner);
        }).assertSuccessful().returnedValue();
    }

    void exactlyThesePartnersAreReturned(final List<HsAdminPartnerEntity> actualResult, final String... partnerTradeNames) {
        assertThat(actualResult)
                .hasSize(partnerTradeNames.length)
                .extracting(HsAdminPartnerEntity::getPerson)
                .extracting(HsAdminPersonEntity::getTradeName)
                .containsExactlyInAnyOrder(partnerTradeNames);
    }

    void allThesePartnersAreReturned(final List<HsAdminPartnerEntity> actualResult, final String... partnerTradeNames) {
        assertThat(actualResult)
                .extracting(HsAdminPartnerEntity::getPerson)
                .extracting(HsAdminPersonEntity::getTradeName)
                .contains(partnerTradeNames);
    }
}
