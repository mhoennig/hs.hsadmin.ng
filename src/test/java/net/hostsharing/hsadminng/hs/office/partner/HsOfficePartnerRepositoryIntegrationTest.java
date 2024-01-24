package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
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
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
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

    @PersistenceContext
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
            final var givenPerson = personRepo.findPersonByOptionalNameLike("First GmbH").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("first contact").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newPartner = toCleanup(HsOfficePartnerEntity.builder()
                        .person(givenPerson)
                        .contact(givenContact)
                        .details(HsOfficePartnerDetailsEntity.builder()
                                .build())
                        .build());
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
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("ErbenBesslerMelBessler", "EBess"))
                    .map(s -> s.replace("forthcontact", "4th"))
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
                final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
                final var newPartner = toCleanup(HsOfficePartnerEntity.builder()
                        .debitorNumberPrefix(22222)
                        .person(givenPerson)
                        .contact(givenContact)
                        .details(HsOfficePartnerDetailsEntity.builder().build())
                        .build());
                return partnerRepo.save(newPartner);
            });

            // then
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_partner#22222:ErbenBesslerMelBessler-forthcontact.admin",
                    "hs_office_partner#22222:ErbenBesslerMelBessler-forthcontact.agent",
                    "hs_office_partner#22222:ErbenBesslerMelBessler-forthcontact.owner",
                    "hs_office_partner#22222:ErbenBesslerMelBessler-forthcontact.tenant",
                    "hs_office_partner#22222:ErbenBesslerMelBessler-forthcontact.guest"));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("ErbenBesslerMelBessler", "EBess"))
                    .map(s -> s.replace("forthcontact", "4th"))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(Array.fromFormatted(
                            initialGrantNames,
                            // owner
                            "{ grant perm * on partner#22222:EBess-4th              to role partner#22222:EBess-4th.owner     by system and assume }",
                            "{ grant perm * on partner_details#22222:EBess-4th-details    to role partner#22222:EBess-4th.owner     by system and assume }",
                            "{ grant role partner#22222:EBess-4th.owner             to role global#global.admin         by system and assume }",

                            // admin
                            "{ grant perm edit on partner#22222:EBess-4th           to role partner#22222:EBess-4th.admin     by system and assume }",
                            "{ grant perm edit on partner_details#22222:EBess-4th-details to role partner#22222:EBess-4th.admin     by system and assume }",
                            "{ grant role partner#22222:EBess-4th.admin             to role partner#22222:EBess-4th.owner     by system and assume }",
                            "{ grant role person#EBess.tenant                       to role partner#22222:EBess-4th.admin     by system and assume }",
                            "{ grant role contact#4th.tenant                        to role partner#22222:EBess-4th.admin     by system and assume }",

                            // agent
                            "{ grant perm view on partner_details#22222:EBess-4th-details to role partner#22222:EBess-4th.agent     by system and assume }",
                            "{ grant role partner#22222:EBess-4th.agent             to role partner#22222:EBess-4th.admin     by system and assume }",
                            "{ grant role partner#22222:EBess-4th.agent             to role person#EBess.admin          by system and assume }",
                            "{ grant role partner#22222:EBess-4th.agent             to role contact#4th.admin           by system and assume }",

                            // tenant
                            "{ grant role partner#22222:EBess-4th.tenant            to role partner#22222:EBess-4th.agent     by system and assume }",
                            "{ grant role person#EBess.guest                        to role partner#22222:EBess-4th.tenant    by system and assume }",
                            "{ grant role contact#4th.guest                         to role partner#22222:EBess-4th.tenant    by system and assume }",

                            // guest
                            "{ grant perm view on partner#22222:EBess-4th           to role partner#22222:EBess-4th.guest     by system and assume }",
                            "{ grant role partner#22222:EBess-4th.guest             to role partner#22222:EBess-4th.tenant    by system and assume }",

                            null));
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
            allThesePartnersAreReturned(
                    result,
                    "partner(IF Third OHG: third contact)",
                    "partner(LP Second e.K.: second contact)",
                    "partner(LP First GmbH: first contact)");
        }

        @Test
        public void normalUser_canViewOnlyRelatedPartners() {
            // given:
            context("person-FirstGmbH@example.com");

            // when:
            final var result = partnerRepo.findPartnerByOptionalNameLike(null);

            // then:
            exactlyThesePartnersAreReturned(result, "partner(LP First GmbH: first contact)");
        }
    }

    @Nested
    class FindByNameLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPartners() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = partnerRepo.findPartnerByOptionalNameLike("third contact");

            // then
            exactlyThesePartnersAreReturned(result, "partner(IF Third OHG: third contact)");
        }
    }

    @Nested
    class UpdatePartner {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canUpdateArbitraryPartner() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler(22222, "Erben Bessler", "fifth contact");
            assertThatPartnerIsVisibleForUserWithRole(
                    givenPartner,
                    "hs_office_partner#22222:ErbenBesslerMelBessler-fifthcontact.admin");
            assertThatPartnerActuallyInDatabase(givenPartner);
            context("superuser-alex@hostsharing.net");
            final var givenNewPerson = personRepo.findPersonByOptionalNameLike("Third OHG").get(0);
            final var givenNewContact = contactRepo.findContactByOptionalLabelLike("sixth contact").get(0);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenPartner.setContact(givenNewContact);
                givenPartner.setPerson(givenNewPerson);
                return toCleanup(partnerRepo.save(givenPartner));
            });

            // then
            result.assertSuccessful();
            assertThatPartnerIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global.admin");
            assertThatPartnerIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_person#ThirdOHG.admin");
            assertThatPartnerIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_person#ErbenBesslerMelBessler.admin");

            partnerRepo.deleteByUuid(givenPartner.getUuid());
        }

        @Test
        public void partnerAgent_canNotUpdateRelatedPartner() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryPartnerBessler(22222, "Erben Bessler", "ninth");
            assertThatPartnerIsVisibleForUserWithRole(
                    givenPartner,
                    "hs_office_partner#22222:ErbenBesslerMelBessler-ninthcontact.agent");
            assertThatPartnerActuallyInDatabase(givenPartner);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net",
                        "hs_office_partner#22222:ErbenBesslerMelBessler-ninthcontact.agent");
                givenPartner.getDetails().setBirthName("new birthname");
                return partnerRepo.save(givenPartner);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_partner_details uuid");
        }

        private void assertThatPartnerActuallyInDatabase(final HsOfficePartnerEntity saved) {
            final var found = partnerRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved).usingRecursiveComparison().isEqualTo(saved);
        }

        private void assertThatPartnerIsVisibleForUserWithRole(
                final HsOfficePartnerEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatPartnerActuallyInDatabase(entity);
            }).assertSuccessful();
        }

        private void assertThatPartnerIsNotVisibleForUserWithRole(
                final HsOfficePartnerEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                final var found = partnerRepo.findByUuid(entity.getUuid());
                assertThat(found).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyPartner() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenPartner = givenSomeTemporaryPartnerBessler(22222, "Erben Bessler", "tenth");

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
            final var givenPartner = givenSomeTemporaryPartnerBessler(22222, "Erben Bessler", "eleventh");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-ErbenBesslerMelBessler@example.com");
                assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent();

                partnerRepo.deleteByUuid(givenPartner.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office_partner");
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
            final var givenPartner = givenSomeTemporaryPartnerBessler(22222, "Erben Bessler", "twelfth");

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

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select c.currenttask, j.targettable, j.targetop
                    from tx_journal j
                    join tx_context c on j.contextId = c.contextId
                    where targettable = 'hs_office_partner';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating partner test-data FirstGmbH-firstcontact, hs_office_partner, INSERT]",
                "[creating partner test-data Seconde.K.-secondcontact, hs_office_partner, INSERT]");
    }

    @AfterEach
    void cleanup() {
        context("superuser-alex@hostsharing.net", null);
        tempPartners.forEach(tempPartner -> {
            System.out.println("DELETING temporary partner: " + tempPartner.toString());
            partnerRepo.deleteByUuid(tempPartner.getUuid());
        });
    }

    private HsOfficePartnerEntity givenSomeTemporaryPartnerBessler(
            final Integer debitorNumberPrefix, final String person, final String contact) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike(person).get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike(contact).get(0);
            final var newPartner = HsOfficePartnerEntity.builder()
                    .debitorNumberPrefix(debitorNumberPrefix)
                    .person(givenPerson)
                    .contact(givenContact)
                    .details(HsOfficePartnerDetailsEntity.builder().build())
                    .build();

            toCleanup(newPartner);

            return partnerRepo.save(newPartner);
        }).assertSuccessful().returnedValue();
    }

    private HsOfficePartnerEntity toCleanup(final HsOfficePartnerEntity tempPartner) {
        tempPartners.add(tempPartner);
        return tempPartner;
    }

    void exactlyThesePartnersAreReturned(final List<HsOfficePartnerEntity> actualResult, final String... partnerNames) {
        assertThat(actualResult)
                .extracting(partnerEntity -> partnerEntity.toString())
                .containsExactlyInAnyOrder(partnerNames);
    }

    void allThesePartnersAreReturned(final List<HsOfficePartnerEntity> actualResult, final String... partnerNames) {
        assertThat(actualResult)
                .extracting(partnerEntity -> partnerEntity.toString())
                .contains(partnerNames);
    }
}
