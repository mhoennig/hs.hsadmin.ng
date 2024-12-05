package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacObjectRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
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
import java.util.List;
import java.util.Objects;

import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacObjectEntity.objectDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.mapper.Array.from;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficePartnerRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficeRelationRealRepository relationRepo;

    @Autowired
    HsOfficePersonRealRepository personRepo;

    @Autowired
    HsOfficeContactRealRepository contactrealRepo;

    @Autowired
    RawRbacObjectRepository rawObjectRepo;

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

    @Nested
    class CreatePartner {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewPartner() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = partnerRepo.count();
            final var partnerRel = givenSomeTemporaryHostsharingPartnerRel("First GmbH", "first contact");

            // when
            final var result = attempt(em, () -> {
                final var newPartner = HsOfficePartnerEntity.builder()
                        .partnerNumber(20031)
                        .partnerRel(partnerRel)
                        .details(HsOfficePartnerDetailsEntity.builder().build())
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
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("ErbenBesslerMelBessler", "EBess"))
                    .map(s -> s.replace("fourthcontact", "4th"))
                    .map(s -> s.replace("hs_office.", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenPartnerPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
                final var givenContact = contactrealRepo.findContactByOptionalCaptionLike("fourth contact").get(0);
                final var givenMandantPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").get(0);

                final var newRelation = HsOfficeRelationRealEntity.builder()
                        .holder(givenPartnerPerson)
                        .type(HsOfficeRelationType.PARTNER)
                        .anchor(givenMandantPerson)
                        .contact(givenContact)
                        .build();
                relationRepo.save(newRelation);

                final var newPartner = HsOfficePartnerEntity.builder()
                        .partnerNumber(20032)
                        .partnerRel(newRelation)
                        .details(HsOfficePartnerDetailsEntity.builder().build())
                        .build();
                return partnerRepo.save(newPartner);
            }).assertSuccessful();

            // then
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(from(
                    initialRoleNames,
                    "hs_office.relation#HostsharingeG-with-PARTNER-ErbenBesslerMelBessler:OWNER",
                    "hs_office.relation#HostsharingeG-with-PARTNER-ErbenBesslerMelBessler:ADMIN",
                    "hs_office.relation#HostsharingeG-with-PARTNER-ErbenBesslerMelBessler:AGENT",
                    "hs_office.relation#HostsharingeG-with-PARTNER-ErbenBesslerMelBessler:TENANT"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("ErbenBesslerMelBessler", "EBess"))
                    .map(s -> s.replace("fourthcontact", "4th"))
                    .map(s -> s.replace("hs_office.", ""))
                    .containsExactlyInAnyOrder(distinct(from(
                            initialGrantNames,

                            // permissions on partner
                            "{ grant perm:partner#P-20032:DELETE to role:relation#HostsharingeG-with-PARTNER-EBess:OWNER by system and assume }",
                            "{ grant perm:partner#P-20032:UPDATE to role:relation#HostsharingeG-with-PARTNER-EBess:ADMIN by system and assume }",
                            "{ grant perm:partner#P-20032:SELECT to role:relation#HostsharingeG-with-PARTNER-EBess:TENANT by system and assume }",

                            // permissions on partner-details
                            "{ grant perm:partner_details#P-20032:DELETE to role:relation#HostsharingeG-with-PARTNER-EBess:OWNER by system and assume }",
                            "{ grant perm:partner_details#P-20032:UPDATE to role:relation#HostsharingeG-with-PARTNER-EBess:AGENT by system and assume }",
                            "{ grant perm:partner_details#P-20032:SELECT to role:relation#HostsharingeG-with-PARTNER-EBess:AGENT by system and assume }",

                            // permissions on partner-relation
                            "{ grant perm:relation#HostsharingeG-with-PARTNER-EBess:DELETE to role:relation#HostsharingeG-with-PARTNER-EBess:OWNER by system and assume }",
                            "{ grant perm:relation#HostsharingeG-with-PARTNER-EBess:UPDATE to role:relation#HostsharingeG-with-PARTNER-EBess:ADMIN by system and assume }",
                            "{ grant perm:relation#HostsharingeG-with-PARTNER-EBess:SELECT to role:relation#HostsharingeG-with-PARTNER-EBess:TENANT by system and assume }",

                            // relation owner
                            "{ grant role:relation#HostsharingeG-with-PARTNER-EBess:OWNER to role:rbac.global#global:ADMIN by system and assume }",
                            "{ grant role:relation#HostsharingeG-with-PARTNER-EBess:OWNER to user:superuser-alex@hostsharing.net by relation#HostsharingeG-with-PARTNER-EBess:OWNER and assume }",

                            // relation admin
                            "{ grant role:relation#HostsharingeG-with-PARTNER-EBess:ADMIN to role:relation#HostsharingeG-with-PARTNER-EBess:OWNER by system and assume }",
                            "{ grant role:relation#HostsharingeG-with-PARTNER-EBess:OWNER to role:person#HostsharingeG:ADMIN by system and assume }",

                            // relation agent
                            "{ grant role:relation#HostsharingeG-with-PARTNER-EBess:AGENT to role:person#EBess:ADMIN by system and assume }",
                            "{ grant role:relation#HostsharingeG-with-PARTNER-EBess:AGENT to role:relation#HostsharingeG-with-PARTNER-EBess:ADMIN by system and assume }",

                            // relation tenant
                            "{ grant role:contact#4th:REFERRER to role:relation#HostsharingeG-with-PARTNER-EBess:TENANT by system and assume }",
                            "{ grant role:person#EBess:REFERRER to role:relation#HostsharingeG-with-PARTNER-EBess:TENANT by system and assume }",
                            "{ grant role:person#HostsharingeG:REFERRER to role:relation#HostsharingeG-with-PARTNER-EBess:TENANT by system and assume }",
                            "{ grant role:relation#HostsharingeG-with-PARTNER-EBess:TENANT to role:contact#4th:ADMIN by system and assume }",
                            "{ grant role:relation#HostsharingeG-with-PARTNER-EBess:TENANT to role:relation#HostsharingeG-with-PARTNER-EBess:AGENT by system and assume }",
                            null)));
        }

        private void assertThatPartnerIsPersisted(final HsOfficePartnerEntity saved) {
            final var found = partnerRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().extracting(Object::toString).isEqualTo(saved.toString());
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
                    "partner(P-10001: LP First GmbH, first contact)",
                    "partner(P-10002: LP Second e.K., second contact)",
                    "partner(P-10003: IF Third OHG, third contact)",
                    "partner(P-10004: LP Fourth eG, fourth contact)",
                    "partner(P-10010: NP Smith, Peter, sixth contact)");
        }

        @Test
        public void normalUser_canViewOnlyRelatedPartners() {
            // given:
            context("person-FirstGmbH@example.com");

            // when:
            final var result = partnerRepo.findPartnerByOptionalNameLike(null);

            // then:
            exactlyThesePartnersAreReturned(result, "partner(P-10001: LP First GmbH, first contact)");
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
            exactlyThesePartnersAreReturned(result, "partner(P-10003: IF Third OHG, third contact)");
        }
    }

    @Nested
    class FindByPartnerNumber {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPartners() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = partnerRepo.findPartnerByPartnerNumber(10001);

            // then
            assertThat(result)
                    .isNotNull()
                    .extracting(Object::toString)
                    .isEqualTo("partner(P-10001: LP First GmbH, first contact)");
        }
    }

    @Nested
    class UpdatePartner {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canUpdateArbitraryPartner() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryHostsharingPartner(20036, "Erben Bessler", "fifth contact");
            assertThatPartnerIsVisibleForUserWithRole(
                    givenPartner,
                    "hs_office.person#ErbenBesslerMelBessler:ADMIN");
            assertThatPartnerActuallyInDatabase(givenPartner);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenPartner.setPartnerRel(givenSomeTemporaryHostsharingPartnerRel("Third OHG", "sixth contact"));
                return partnerRepo.save(givenPartner);
            });

            // then
            result.assertSuccessful();

            assertThatPartnerIsVisibleForUserWithRole(
                    givenPartner,
                    "rbac.global#global:ADMIN");
            assertThatPartnerIsVisibleForUserWithRole(
                    givenPartner,
                    "hs_office.person#ThirdOHG:ADMIN");
            assertThatPartnerIsNotVisibleForUserWithRole(
                    givenPartner,
                    "hs_office.person#ErbenBesslerMelBessler:ADMIN");
        }

        @Test
        public void partnerRelationAgent_canUpdateRelatedPartner() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryHostsharingPartner(20037, "Erben Bessler", "ninth");
            assertThatPartnerIsVisibleForUserWithRole(
                    givenPartner,
                    "hs_office.person#ErbenBesslerMelBessler:ADMIN");
            assertThatPartnerActuallyInDatabase(givenPartner);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net",
                        "hs_office.person#ErbenBesslerMelBessler:ADMIN");
                givenPartner.getDetails().setBirthName("new birthname");
                return partnerRepo.save(givenPartner);
            });

            // then
            result.assertSuccessful();
        }

        @Test
        public void partnerRelationTenant_canNotUpdateRelatedPartner() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenPartner = givenSomeTemporaryHostsharingPartner(20037, "Erben Bessler", "ninth");
            assertThatPartnerIsVisibleForUserWithRole(
                    givenPartner,
                    "hs_office.person#ErbenBesslerMelBessler:ADMIN");
            assertThatPartnerActuallyInDatabase(givenPartner);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net",
                        "hs_office.relation#HostsharingeG-with-PARTNER-ErbenBesslerMelBessler:TENANT");
                givenPartner.getDetails().setBirthName("new birthname");
                return partnerRepo.save(givenPartner);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "ERROR: [403] insert into hs_office.partner_details ",
                    " not allowed for current subjects {hs_office.relation#HostsharingeG-with-PARTNER-ErbenBesslerMelBessler:TENANT}");
        }

        private void assertThatPartnerActuallyInDatabase(final HsOfficePartnerEntity saved) {
            final var found = partnerRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved).extracting(HsOfficePartnerEntity::toString).isEqualTo(saved.toString());
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
            final var givenPartner = givenSomeTemporaryHostsharingPartner(20032, "Erben Bessler", "tenth");

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
            final var givenPartner = givenSomeTemporaryHostsharingPartner(20033, "Erben Bessler", "eleventh");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-ErbenBesslerMelBessler@example.com");
                assertThat(partnerRepo.findByUuid(givenPartner.getUuid())).isPresent();

                partnerRepo.deleteByUuid(givenPartner.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office.partner");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return partnerRepo.findByUuid(givenPartner.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingAPartnerAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialObjects = from(objectDisplaysOf(rawObjectRepo.findAll()));
            final var initialRoleNames = from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenPartner = givenSomeTemporaryHostsharingPartner(20034, "Erben Bessler", "twelfth");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return partnerRepo.deleteByUuid(givenPartner.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(objectDisplaysOf(rawObjectRepo.findAll())).containsExactlyInAnyOrder(initialObjects);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp, targetdelta->>'partnernumber'
                    from base.tx_journal_v
                    where targettable = 'hs_office.partner';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating partner test-data , hs_office.partner, INSERT, 10001]",
                "[creating partner test-data , hs_office.partner, INSERT, 10002]",
                "[creating partner test-data , hs_office.partner, INSERT, 10003]",
                "[creating partner test-data , hs_office.partner, INSERT, 10004]",
                "[creating partner test-data , hs_office.partner, INSERT, 10010]");
    }

    private HsOfficePartnerEntity givenSomeTemporaryHostsharingPartner(
            final Integer partnerNumber, final String person, final String contact) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var partnerRel = givenSomeTemporaryHostsharingPartnerRel(person, contact);

            final var newPartner = HsOfficePartnerEntity.builder()
                    .partnerNumber(partnerNumber)
                    .partnerRel(partnerRel)
                    .details(HsOfficePartnerDetailsEntity.builder().build())
                    .build();

            return partnerRepo.save(newPartner);
        }).assertSuccessful().returnedValue();
    }

    private HsOfficeRelationRealEntity givenSomeTemporaryHostsharingPartnerRel(final String person, final String contact) {
        final var givenMandantorPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").get(0);
        final var givenPartnerPerson = personRepo.findPersonByOptionalNameLike(person).get(0);
        final var givenContact = contactrealRepo.findContactByOptionalCaptionLike(contact).get(0);

        final var partnerRel = HsOfficeRelationRealEntity.builder()
                .holder(givenPartnerPerson)
                .type(HsOfficeRelationType.PARTNER)
                .anchor(givenMandantorPerson)
                .contact(givenContact)
                .build();
        relationRepo.save(partnerRel).load();
        return partnerRel;
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

    @AfterEach
    void cleanup() {
        cleanupAllNew(HsOfficePartnerEntity.class);
    }

    private String[] distinct(final String[] strings) {
        // TODO: alternatively cleanup all rbac objects in @AfterEach?
        return Arrays.stream(strings).filter(Objects::nonNull).distinct().toList().toArray(new String[0]);
    }
}
