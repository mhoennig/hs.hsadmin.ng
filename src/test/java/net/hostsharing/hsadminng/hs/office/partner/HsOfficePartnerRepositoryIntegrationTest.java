package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { HsOfficePartnerRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsOfficePartnerRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @Autowired
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockBean
    HttpServletRequest request;

    Set<HsOfficePartnerEntity> tempPartners = new HashSet<>();

    @Nested
    class CreatePartner {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewPartner() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = partnerRepo.count();
            final var givenPerson = personRepo.findPersonByOptionalNameLike("First Impressions GmbH").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("first contact").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newPartner = HsOfficePartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(givenPerson)
                        .contact(givenContact)
                        .build();
                return partnerRepo.save(newPartner);
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficePartnerEntity::getUuid).isNotNull();
            assertThatPartnerIsPersisted(result.returnedValue());
            assertThat(partnerRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> {
                final var givenPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
                final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
                final var newPartner = HsOfficePartnerEntity.builder()
                        .uuid(UUID.randomUUID())
                        .person(givenPerson)
                        .contact(givenContact)
                        .build();
                return partnerRepo.save(newPartner);
            });

            // then
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_partner#ErbenBesslerMelBessler-forthcontact.admin",
                    "hs_office_partner#ErbenBesslerMelBessler-forthcontact.owner",
                    "hs_office_partner#ErbenBesslerMelBessler-forthcontact.tenant"));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialGrantNames,
                    "{ grant role hs_office_partner#ErbenBesslerMelBessler-forthcontact.owner to role global#global.admin by system and assume }",
                    "{ grant role hs_office_partner#ErbenBesslerMelBessler-forthcontact.tenant to role hs_office_contact#forthcontact.admin by system and assume }",
                    "{ grant perm edit on hs_office_partner#ErbenBesslerMelBessler-forthcontact to role hs_office_partner#ErbenBesslerMelBessler-forthcontact.admin by system and assume }",
                    "{ grant role hs_office_partner#ErbenBesslerMelBessler-forthcontact.tenant to role hs_office_partner#ErbenBesslerMelBessler-forthcontact.admin by system and assume }",
                    "{ grant perm * on hs_office_partner#ErbenBesslerMelBessler-forthcontact to role hs_office_partner#ErbenBesslerMelBessler-forthcontact.owner by system and assume }",
                    "{ grant role hs_office_partner#ErbenBesslerMelBessler-forthcontact.admin to role hs_office_partner#ErbenBesslerMelBessler-forthcontact.owner by system and assume }",
                    "{ grant perm view on hs_office_partner#ErbenBesslerMelBessler-forthcontact to role hs_office_partner#ErbenBesslerMelBessler-forthcontact.tenant by system and assume }",
                    "{ grant role hs_office_contact#forthcontact.tenant to role hs_office_partner#ErbenBesslerMelBessler-forthcontact.tenant by system and assume }",
                    "{ grant role hs_office_person#ErbenBesslerMelBessler.tenant to role hs_office_partner#ErbenBesslerMelBessler-forthcontact.tenant by system and assume }",
                    "{ grant role hs_office_partner#ErbenBesslerMelBessler-forthcontact.tenant to role hs_office_person#ErbenBesslerMelBessler.admin by system and assume }"));
        }

        private void assertThatPartnerIsPersisted(final HsOfficePartnerEntity saved) {
            final var found = partnerRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllPartners {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPartners() {
            // given
            context("superuser-alex@hostsharing.net");

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
            context("superuser-alex@hostsharing.net");

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
            context("superuser-alex@hostsharing.net", null);
            final var givenPartner = givenSomeTemporaryPartnerBessler();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                partnerRepo.deleteByUuid(givenPartner.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return partnerRepo.findByUuid(givenPartner.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void nonGlobalAdmin_canNotDeleteTheirRelatedPartner() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenPartner = toCleanup(givenSomeTemporaryPartnerBessler());

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-ErbenBesslerMelBessler@example.com");
                assumeThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent();

                partnerRepo.deleteByUuid(givenPartner.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] User person-ErbenBesslerMelBessler@example.com not allowed to delete partner");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return partnerRepo.findByUuid(givenPartner.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingAPartnerAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(roleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(grantDisplaysOf(rawGrantRepo.findAll()));
            final var givenPartner = givenSomeTemporaryPartnerBessler();
            assumeThat(rawRoleRepo.findAll().size()).as("unexpected number of roles created")
                    .isEqualTo(initialRoleNames.length + 3);
            assumeThat(rawGrantRepo.findAll().size()).as("unexpected number of grants created")
                    .isEqualTo(initialGrantNames.length + 10);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return partnerRepo.deleteByUuid(givenPartner.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    private HsOfficePartnerEntity givenSomeTemporaryPartnerBessler() {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
            final var newPartner = HsOfficePartnerEntity.builder()
                    .uuid(UUID.randomUUID())
                    .person(givenPerson)
                    .contact(givenContact)
                    .build();

            return partnerRepo.save(newPartner);
        }).assertSuccessful().returnedValue();
    }

    private HsOfficePartnerEntity toCleanup(final HsOfficePartnerEntity tempPartner) {
        tempPartners.add(tempPartner);
        return tempPartner;
    }

    @AfterEach
    void cleanup() {
        context("superuser-alex@hostsharing.net", null);
        tempPartners.forEach(tempPartner -> {
            System.out.println("DELETING temporary partner: " + tempPartner.getDisplayName());
            final var count = partnerRepo.deleteByUuid(tempPartner.getUuid());
            assertThat(count).isGreaterThan(0);
        });
    }

    void exactlyThesePartnersAreReturned(final List<HsOfficePartnerEntity> actualResult, final String... partnerTradeNames) {
        assertThat(actualResult)
                .hasSize(partnerTradeNames.length)
                .extracting(HsOfficePartnerEntity::getPerson)
                .extracting(HsOfficePersonEntity::getTradeName)
                .containsExactlyInAnyOrder(partnerTradeNames);
    }

    void allThesePartnersAreReturned(final List<HsOfficePartnerEntity> actualResult, final String... partnerTradeNames) {
        assertThat(actualResult)
                .extracting(HsOfficePartnerEntity::getPerson)
                .extracting(HsOfficePersonEntity::getTradeName)
                .contains(partnerTradeNames);
    }
}
