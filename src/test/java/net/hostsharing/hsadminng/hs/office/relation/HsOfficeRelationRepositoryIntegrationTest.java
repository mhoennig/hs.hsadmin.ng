package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.hsadminng.rbac.test.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
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

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.UNINCORPORATED_FIRM;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficeRelationRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeRelationRepository relationRepo;

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

    @Nested
    class CreateRelation {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewRelation() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = relationRepo.count();
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                    .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                    .findFirst().orElseThrow();
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Paul").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth contact").stream()
                    .findFirst().orElseThrow();

            // when
            final var result = attempt(em, () -> {
                final var newRelation = HsOfficeRelationEntity.builder()
                        .anchor(givenAnchorPerson)
                        .holder(givenHolderPerson)
                        .type(HsOfficeRelationType.SUBSCRIBER)
                        .mark("operations-announce")
                        .contact(givenContact)
                        .build();
                return toCleanup(relationRepo.save(newRelation));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeRelationEntity::getUuid).isNotNull();
            assertThatRelationIsPersisted(result.returnedValue());
            assertThat(relationRepo.count()).isEqualTo(count + 1);
            final var stored = relationRepo.findByUuid(result.returnedValue().getUuid());
            assertThat(stored).isNotEmpty().map(HsOfficeRelationEntity::toString).get()
                    .isEqualTo("rel(anchor='UF Erben Bessler', type='SUBSCRIBER', mark='operations-announce', holder='NP Winkler, Paul', contact='fourth contact')");
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> {
                final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                        .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                        .findFirst().orElseThrow();
                final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Bert").stream()
                        .filter(p -> p.getPersonType() == NATURAL_PERSON)
                        .findFirst().orElseThrow();
                final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth contact").stream()
                        .findFirst().orElseThrow();
                final var newRelation = HsOfficeRelationEntity.builder()
                        .anchor(givenAnchorPerson)
                        .holder(givenHolderPerson)
                        .type(HsOfficeRelationType.REPRESENTATIVE)
                        .contact(givenContact)
                        .build();
                return toCleanup(relationRepo.save(newRelation));
            });

            // then
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER",
                    "hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN",
                    "hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT",
                    "hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.fromFormatted(
                    initialGrantNames,
                    // TODO: this grant should only be created for DEBITOR-Relationships, thus the RBAC DSL needs to support conditional grants
                    "{ grant perm:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:INSERT>hs_office_sepamandate to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",

                    "{ grant perm:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:DELETE to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER by system and assume }",
                    "{ grant role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER to role:global#global:ADMIN by system and assume }",
                    "{ grant role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER to user:superuser-alex@hostsharing.net by hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER and assume }",

                    "{ grant perm:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:UPDATE to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",
                    "{ grant role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER by system and assume }",
                    "{ grant role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER to role:hs_office_person#BesslerBert:ADMIN by system and assume }",
                    "{ grant role:hs_office_person#ErbenBesslerMelBessler:OWNER to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",

                    "{ grant role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT to role:hs_office_person#ErbenBesslerMelBessler:ADMIN by system and assume }",
                    "{ grant role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",

                    "{ grant perm:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:SELECT to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT by system and assume }",
                    "{ grant role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT by system and assume }",
                    "{ grant role:hs_office_person#BesslerBert:REFERRER to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT by system and assume }",
                    "{ grant role:hs_office_person#ErbenBesslerMelBessler:REFERRER to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT by system and assume }",
                    "{ grant role:hs_office_contact#fourthcontact:REFERRER to role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT by system and assume }",

                    // REPRESENTATIVE holder person -> (represented) anchor person
                    "{ grant role:hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT to role:hs_office_contact#fourthcontact:ADMIN by system and assume }",
                    null)
            );
        }

        private void assertThatRelationIsPersisted(final HsOfficeRelationEntity saved) {
            final var found = relationRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllRelations {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllRelationsOfArbitraryPerson() {
            // given
            context("superuser-alex@hostsharing.net");
            final var person = personRepo.findPersonByOptionalNameLike("Smith").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();

            // when
            final var result = relationRepo.findRelationRelatedToPersonUuid(person.getUuid());

            // then
            allTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Hostsharing eG', type='PARTNER', holder='NP Smith, Peter', contact='sixth contact')",
                    "rel(anchor='LP Second e.K.', type='REPRESENTATIVE', holder='NP Smith, Peter', contact='second contact')",
                    "rel(anchor='IF Third OHG', type='SUBSCRIBER', mark='members-announce', holder='NP Smith, Peter', contact='third contact')");
        }

        @Test
        public void normalUser_canViewRelationsOfOwnedPersons() {
            // given:
            context("person-SmithPeter@example.com");
            final var person = personRepo.findPersonByOptionalNameLike("Smith").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();

            // when:
            final var result = relationRepo.findRelationRelatedToPersonUuid(person.getUuid());

            // then:
            exactlyTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Second e.K.', type='REPRESENTATIVE', holder='NP Smith, Peter', contact='second contact')",
                    "rel(anchor='IF Third OHG', type='SUBSCRIBER', mark='members-announce', holder='NP Smith, Peter', contact='third contact')",
                    "rel(anchor='LP Hostsharing eG', type='PARTNER', holder='NP Smith, Peter', contact='sixth contact')",
                    "rel(anchor='NP Smith, Peter', type='DEBITOR', holder='NP Smith, Peter', contact='third contact')");
        }
    }

    @Nested
    class UpdateRelation {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canUpdateContactOfArbitraryRelation() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Bert", "fifth contact");
            assertThatRelationActuallyInDatabase(givenRelation);
            assertThatRelationIsVisibleForUserWithRole(
                    givenRelation,
                    "hs_office_person#ErbenBesslerMelBessler:ADMIN");
            context("superuser-alex@hostsharing.net");
            final var givenContact = contactRepo.findContactByOptionalLabelLike("sixth contact").stream().findFirst().orElseThrow();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenRelation.setContact(givenContact);
                return toCleanup(relationRepo.save(givenRelation));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue().getContact().getLabel()).isEqualTo("sixth contact");
            assertThatRelationIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global:ADMIN");
            assertThatRelationIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#sixthcontact:ADMIN");

            assertThatRelationIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#fifthcontact:ADMIN");

            relationRepo.deleteByUuid(givenRelation.getUuid());
        }

        @Test
        public void relationAgent_canSelectButNotUpdateRelatedRelation() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "eighth");
            assertThatRelationIsVisibleForUserWithRole(
                    givenRelation,
                    "hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerAnita:AGENT");
            assertThatRelationActuallyInDatabase(givenRelation);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerAnita:AGENT");
                givenRelation.setContact(null);
                return relationRepo.save(givenRelation);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_relation uuid");
        }

        @Test
        public void contactAdmin_canNotUpdateRelatedRelation() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "ninth");
            assertThatRelationIsVisibleForUserWithRole(
                    givenRelation,
                    "hs_office_contact#ninthcontact:ADMIN");
            assertThatRelationActuallyInDatabase(givenRelation);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_contact#ninthcontact:ADMIN");
                givenRelation.setContact(null); // TODO
                return relationRepo.save(givenRelation);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_relation uuid");
        }

        private void assertThatRelationActuallyInDatabase(final HsOfficeRelationEntity saved) {
            final var found = relationRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get()
                    .isNotSameAs(saved)
                    .usingRecursiveComparison().ignoringFields("version").isEqualTo(saved);
        }

        private void assertThatRelationIsVisibleForUserWithRole(
                final HsOfficeRelationEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatRelationActuallyInDatabase(entity);
            }).assertSuccessful();
        }

        private void assertThatRelationIsNotVisibleForUserWithRole(
                final HsOfficeRelationEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                final var found = relationRepo.findByUuid(entity.getUuid());
                assertThat(found).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyRelation() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "tenth");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                relationRepo.deleteByUuid(givenRelation.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return relationRepo.findByUuid(givenRelation.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void contactUser_canViewButNotDeleteTheirRelatedRelation() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "eleventh");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("contact-admin@eleventhcontact.example.com");
                assertThat(relationRepo.findByUuid(givenRelation.getUuid())).isPresent();
                relationRepo.deleteByUuid(givenRelation.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office_relation");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return relationRepo.findByUuid(givenRelation.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingARelationAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "twelfth");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return relationRepo.deleteByUuid(givenRelation.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp
                    from tx_journal_v
                    where targettable = 'hs_office_relation';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating relation test-data HostsharingeG-FirstGmbH, hs_office_relation, INSERT]",
                "[creating relation test-data FirstGmbH-Firby, hs_office_relation, INSERT]");
    }

    private HsOfficeRelationEntity givenSomeTemporaryRelationBessler(final String holderPerson, final String contact) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike(holderPerson).get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike(contact).get(0);
            final var newRelation = HsOfficeRelationEntity.builder()
                    .type(HsOfficeRelationType.REPRESENTATIVE)
                    .anchor(givenAnchorPerson)
                    .holder(givenHolderPerson)
                    .contact(givenContact)
                    .build();

            return toCleanup(relationRepo.save(newRelation));
        }).assertSuccessful().returnedValue();
    }

    void exactlyTheseRelationsAreReturned(
            final List<HsOfficeRelationEntity> actualResult,
            final String... relationNames) {
        assertThat(actualResult)
                .extracting(HsOfficeRelationEntity::toString)
                .containsExactlyInAnyOrder(relationNames);
    }

    void allTheseRelationsAreReturned(
            final List<HsOfficeRelationEntity> actualResult,
            final String... relationNames) {
        assertThat(actualResult)
                .extracting(HsOfficeRelationEntity::toString)
                .contains(relationNames);
    }
}
