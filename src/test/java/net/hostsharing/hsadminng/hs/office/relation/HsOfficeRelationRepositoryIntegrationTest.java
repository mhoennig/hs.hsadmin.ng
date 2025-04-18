package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.UNINCORPORATED_FIRM;
import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.role.RbacRoleType.ADMIN;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
@Tag("officeIntegrationTest")
class HsOfficeRelationRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeRelationRbacRepository relationRbacRepo;

    @Autowired
    HsOfficeRelationRealRepository relationRealRepo;

    @Autowired
    HsOfficePersonRealRepository personRepo;

    @Autowired
    HsOfficeContactRealRepository contactRealRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockitoBean
    HttpServletRequest request;

    @Nested
    class AssumeRelationRole {

        // TODO.test: these tests would be better placed in the rbac module,
        //  but for this we need an extra long object-idname in the rbac test data

        @Test
        public void testHostsharingAdminCanAssumeRelationRoleWithLongIdName() {
            context(
                    "superuser-alex@hostsharing.net",
                    "hs_office.relation#HostsharingeG-with-PARTNER-PeterSmith-TheSecondHandandThriftStores-n-Shippinge.K.SmithPeter:AGENT");
        }

        @Test
        public void testHostsharingAdminCanAssumeRelationRoleWithUuid() {
            final var relationUuid = relationRealRepo.findRelationRelatedToPersonUuidRelationTypeMarkPersonAndContactData(
                    null, HsOfficeRelationType.PARTNER, null, "%Second%", null)
                    .stream().reduce(Reducer::toSingleElement).orElseThrow().getUuid();

            context("superuser-alex@hostsharing.net", "hs_office.relation#" + relationUuid + ":AGENT");
        }
    }

    @Nested
    class CreateRelation {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewRelation() {
            // given
            context("superuser-alex@hostsharing.net");

            final var count = relationRbacRepo.count();
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                    .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                    .findFirst().orElseThrow();
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Paul").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth contact").stream()
                    .findFirst().orElseThrow();

            // when
            final var result = attempt(
                    em, () -> {
                        final var newRelation = HsOfficeRelationRbacEntity.builder()
                                .anchor(givenAnchorPerson)
                                .holder(givenHolderPerson)
                                .type(HsOfficeRelationType.SUBSCRIBER)
                                .mark("operations-announce")
                                .contact(givenContact)
                                .build();
                        return toCleanup(relationRbacRepo.save(newRelation));
                    });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeRelation::getUuid).isNotNull();
            assertThatRelationIsPersisted(result.returnedValue());
            assertThat(relationRbacRepo.count()).isEqualTo(count + 1);
            final var stored = relationRbacRepo.findByUuid(result.returnedValue().getUuid());
            assertThat(stored).isNotEmpty().map(HsOfficeRelation::toString).get()
                    .isEqualTo(
                            "rel(anchor='UF Erben Bessler', type='SUBSCRIBER', mark='operations-announce', holder='NP Winkler, Paul', contact='fourth contact')");
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(
                    em, () -> {
                        final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                                .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                                .findFirst().orElseThrow();
                        final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Bert").stream()
                                .filter(p -> p.getPersonType() == NATURAL_PERSON)
                                .findFirst().orElseThrow();
                        final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth contact").stream()
                                .findFirst().orElseThrow();
                        final var newRelation = HsOfficeRelationRbacEntity.builder()
                                .anchor(givenAnchorPerson)
                                .holder(givenHolderPerson)
                                .type(HsOfficeRelationType.REPRESENTATIVE)
                                .contact(givenContact)
                                .build();
                        return toCleanup(relationRbacRepo.save(newRelation));
                    });

            // then
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER",
                    "hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN",
                    "hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT",
                    "hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.fromFormatted(
                    initialGrantNames,

                    "{ grant perm:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:DELETE to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER to role:rbac.global#global:ADMIN by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER to user:superuser-alex@hostsharing.net by hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER and assume }",

                    "{ grant perm:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:UPDATE to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER to role:hs_office.person#BesslerBert:ADMIN by system and assume }",
                    "{ grant role:hs_office.person#ErbenBesslerMelBessler:OWNER to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",

                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT to role:hs_office.person#ErbenBesslerMelBessler:ADMIN by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",

                    "{ grant perm:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:SELECT to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT by system and assume }",
                    "{ grant role:hs_office.person#BesslerBert:REFERRER to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT by system and assume }",
                    "{ grant role:hs_office.person#ErbenBesslerMelBessler:REFERRER to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT by system and assume }",
                    "{ grant role:hs_office.contact#fourthcontact:REFERRER to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT by system and assume }",

                    // REPRESENTATIVE holder person -> (represented) anchor person
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:TENANT to role:hs_office.contact#fourthcontact:ADMIN by system and assume }",
                    null)
            );
        }

        private void assertThatRelationIsPersisted(final HsOfficeRelation saved) {
            final var found = relationRbacRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().extracting(Object::toString).isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindRelations {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllRelationsOfArbitraryPerson() {
            // given
            context("superuser-alex@hostsharing.net");
            final var person = personRepo.findPersonByOptionalNameLike("Smith").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();

            // when
            final var result = relationRbacRepo.findRelationRelatedToPersonUuid(person.getUuid());

            // then
            allTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Hostsharing eG', type='PARTNER', holder='NP Smith, Peter', contact='sixth contact')",
                    "rel(anchor='LP Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', type='REPRESENTATIVE', holder='NP Smith, Peter', contact='second contact')",
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
            final var result = relationRbacRepo.findRelationRelatedToPersonUuidRelationTypeMarkPersonAndContactData(
                    person.getUuid(),
                    null,
                    null,
                    null,
                    null);

            // then:
            exactlyTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', type='REPRESENTATIVE', holder='NP Smith, Peter', contact='second contact')",
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
                    "hs_office.person#ErbenBesslerMelBessler:ADMIN");
            context("superuser-alex@hostsharing.net");
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("sixth contact")
                    .stream()
                    .findFirst()
                    .orElseThrow();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenRelation.setContact(givenContact);
                return toCleanup(relationRbacRepo.save(givenRelation).load());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue().getContact().getCaption()).isEqualTo("sixth contact");
            assertThatRelationIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "rbac.global#global:ADMIN");
            assertThatRelationIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office.contact#sixthcontact:ADMIN");

            assertThatRelationIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office.contact#fifthcontact:ADMIN");
        }

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canUpdateHolderOfArbitraryRelation() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Bert", "fifth contact");
            final var oldHolderPerson = givenRelation.getHolder();
            final var newHolderPerson = personRepo.findPersonByOptionalNameLike("Paul").getFirst();
            assertThatRelationActuallyInDatabase(givenRelation);
            assertThatRelationIsVisibleForUserWithRole(
                    givenRelation,
                    givenRelation.getHolder().roleId(ADMIN));

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenRelation.setHolder(newHolderPerson);
                return toCleanup(relationRbacRepo.save(givenRelation).load());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue().getHolder().getGivenName()).isEqualTo("Paul");
            assertThatRelationIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "rbac.global#global:ADMIN");
            assertThatRelationIsVisibleForUserWithRole(
                    result.returnedValue(),
                    newHolderPerson.roleId(ADMIN));
            assertThatRelationIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    oldHolderPerson.roleId(ADMIN));
        }

        @Test
        public void relationAgent_canSelectButNotUpdateRelatedRelation() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "eighth");
            assertThatRelationIsVisibleForUserWithRole(
                    givenRelation,
                    "hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerAnita:AGENT");
            assertThatRelationActuallyInDatabase(givenRelation);
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("sixth contact")
                    .stream()
                    .findFirst()
                    .orElseThrow();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context(
                        "superuser-alex@hostsharing.net",
                        "hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerAnita:AGENT");
                givenRelation.setContact(givenContact);
                return relationRbacRepo.save(givenRelation);
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office.relation uuid");
        }

        @Test
        public void contactAdmin_canNotUpdateRelatedRelation() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "ninth");
            assertThatRelationIsVisibleForUserWithRole(
                    givenRelation,
                    "hs_office.contact#ninthcontact:ADMIN");
            assertThatRelationActuallyInDatabase(givenRelation);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office.contact#ninthcontact:ADMIN");
                givenRelation.setContact(null); // TODO
                return relationRbacRepo.save(givenRelation);
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office.relation uuid");
        }

        private void assertThatRelationActuallyInDatabase(final HsOfficeRelation saved) {
            final var found = relationRbacRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get()
                    .isNotSameAs(saved)
                    .extracting(HsOfficeRelation::toString)
                    .isEqualTo(saved.toString());
        }

        private void assertThatRelationIsVisibleForUserWithRole(
                final HsOfficeRelation entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatRelationActuallyInDatabase(entity);
            }).assertSuccessful();
        }

        private void assertThatRelationIsNotVisibleForUserWithRole(
                final HsOfficeRelation entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                final var found = relationRbacRepo.findByUuid(entity.getUuid());
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
                relationRbacRepo.deleteByUuid(givenRelation.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return relationRbacRepo.findByUuid(givenRelation.getUuid());
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
                assertThat(relationRbacRepo.findByUuid(givenRelation.getUuid())).isPresent();
                relationRbacRepo.deleteByUuid(givenRelation.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office.relation");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return relationRbacRepo.findByUuid(givenRelation.getUuid());
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
                return relationRbacRepo.deleteByUuid(givenRelation.getUuid());
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
                select currentTask, targetTable, targetOp, targetdelta->>'mark'
                    from base.tx_journal_v
                    where targettable = 'hs_office.relation';
                """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating relation test-data, hs_office.relation, INSERT, members-announce]");
    }

    private HsOfficeRelationRbacEntity givenSomeTemporaryRelationBessler(final String holderPerson, final String contact) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").getFirst();
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike(holderPerson).getFirst();
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike(contact).getFirst();
            final var newRelation = HsOfficeRelationRbacEntity.builder()
                    .type(HsOfficeRelationType.REPRESENTATIVE)
                    .anchor(givenAnchorPerson)
                    .holder(givenHolderPerson)
                    .contact(givenContact)
                    .build();

            return toCleanup(relationRbacRepo.save(newRelation));
        }).assertSuccessful().returnedValue();
    }

    void exactlyTheseRelationsAreReturned(
            final List<HsOfficeRelationRbacEntity> actualResult,
            final String... relationNames) {
        assertThat(actualResult)
                .extracting(HsOfficeRelation::toString)
                .containsExactlyInAnyOrder(relationNames);
    }

    void allTheseRelationsAreReturned(
            final List<HsOfficeRelationRbacEntity> actualResult,
            final String... relationNames) {
        assertThat(actualResult)
                .extracting(HsOfficeRelation::toString)
                .contains(relationNames);
    }
}
