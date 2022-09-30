package net.hostsharing.hsadminng.hs.office.relationship;

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
@ComponentScan(basePackageClasses = { HsOfficeRelationshipRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsOfficeRelationshipRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficeRelationshipRepository relationshipRepo;

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

    Set<HsOfficeRelationshipEntity> tempRelationships = new HashSet<>();

    @Nested
    class CreateRelationship {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewRelationship() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = relationshipRepo.count();
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").get(0);
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Anita").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newRelationship = toCleanup(HsOfficeRelationshipEntity.builder()
                        .uuid(UUID.randomUUID())
                        .relAnchor(givenAnchorPerson)
                        .relHolder(givenHolderPerson)
                        .relType(HsOfficeRelationshipType.JOINT_AGENT)
                        .contact(givenContact)
                        .build());
                return relationshipRepo.save(newRelationship);
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeRelationshipEntity::getUuid).isNotNull();
            assertThatRelationshipIsPersisted(result.returnedValue());
            assertThat(relationshipRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> {
                final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").get(0);
                final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Anita").get(0);
                final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
                final var newRelationship = toCleanup(HsOfficeRelationshipEntity.builder()
                        .uuid(UUID.randomUUID())
                        .relAnchor(givenAnchorPerson)
                        .relHolder(givenHolderPerson)
                        .relType(HsOfficeRelationshipType.JOINT_AGENT)
                        .contact(givenContact)
                        .build());
                return relationshipRepo.save(newRelationship);
            });

            // then
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.admin",
                    "hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.owner",
                    "hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.tenant"));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.fromSkippingNull(
                    initialGrantNames,

                    "{ grant perm * on hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita to role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.owner by system and assume }",
                    "{ grant role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.owner to role global#global.admin by system and assume }",
                    "{ grant role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.owner to role hs_office_person#BesslerAnita.admin by system and assume }",

                    "{ grant perm edit on hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita to role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.admin by system and assume }",
                    "{ grant role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.admin to role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.owner by system and assume }",

                    "{ grant perm view on hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita to role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.tenant by system and assume }",
                    "{ grant role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.tenant to role hs_office_contact#forthcontact.admin by system and assume }",
                    "{ grant role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.tenant to role hs_office_person#BesslerAnita.admin by system and assume }",

                    "{ grant role hs_office_contact#forthcontact.tenant to role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.tenant by system and assume }",
                    "{ grant role hs_office_person#BesslerAnita.tenant to role hs_office_relationship#BesslerAnita-with-JOINT_AGENT-BesslerAnita.tenant by system and assume }",
                    null)
            );
        }

        private void assertThatRelationshipIsPersisted(final HsOfficeRelationshipEntity saved) {
            final var found = relationshipRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllRelationships {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllRelationshipsOfArbitraryPerson() {
            // given
            context("superuser-alex@hostsharing.net");
            final var person = personRepo.findPersonByOptionalNameLike("Smith").stream().findFirst().orElseThrow();

            // when
            final var result = relationshipRepo.findRelationshipRelatedToPersonUuid(person.getUuid());

            // then
            allTheseRelationshipsAreReturned(
                    result,
                    "rel(relAnchor='First GmbH', relType='SOLE_AGENT', relHolder='Smith, Peter', contact='first contact')",
                    "rel(relAnchor='Third OHG', relType='SOLE_AGENT', relHolder='Smith, Peter', contact='third contact')",
                    "rel(relAnchor='Second e.K.', relType='SOLE_AGENT', relHolder='Smith, Peter', contact='second contact')");
        }

        @Test
        public void normalUser_canViewRelationshipsOfOwnedPersons() {
            // given:
            context("person-FirstGmbH@example.com");
            final var person = personRepo.findPersonByOptionalNameLike("First").stream().findFirst().orElseThrow();

            // when:
            final var result = relationshipRepo.findRelationshipRelatedToPersonUuid(person.getUuid());

            // then:
            exactlyTheseRelationshipsAreReturned(
                    result,
                    "rel(relAnchor='First GmbH', relType='SOLE_AGENT', relHolder='Smith, Peter', contact='first contact')");
        }
    }

    @Nested
    class UpdateRelationship {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canUpdateContactOfArbitraryRelationship() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelationship = givenSomeTemporaryRelationshipBessler(
                    "Anita", "fifth contact");
            assertThatRelationshipIsVisibleForUserWithRole(
                    givenRelationship,
                    "hs_office_person#ErbenBesslerMelBessler.admin");
            assertThatRelationshipActuallyInDatabase(givenRelationship);
            context("superuser-alex@hostsharing.net");
            final var givenContact = contactRepo.findContactByOptionalLabelLike("sixth contact").get(0);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenRelationship.setContact(givenContact);
                return toCleanup(relationshipRepo.save(givenRelationship));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue().getContact().getLabel()).isEqualTo("sixth contact");
            assertThatRelationshipIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global.admin");
            assertThatRelationshipIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#sixthcontact.admin");

            assertThatRelationshipIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#fifthcontact.admin");

            relationshipRepo.deleteByUuid(givenRelationship.getUuid());
        }

        @Test
        public void relHolderAdmin_canNotUpdateRelatedRelationship() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelationship = givenSomeTemporaryRelationshipBessler(
                    "Anita", "eighth");
            assertThatRelationshipIsVisibleForUserWithRole(
                    givenRelationship,
                    "hs_office_person#BesslerAnita.admin");
            assertThatRelationshipActuallyInDatabase(givenRelationship);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_person#BesslerAnita.admin");
                givenRelationship.setContact(null);
                return relationshipRepo.save(givenRelationship);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_relationship uuid");
        }

        @Test
        public void contactAdmin_canNotUpdateRelatedRelationship() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenRelationship = givenSomeTemporaryRelationshipBessler(
                    "Anita", "ninth");
            assertThatRelationshipIsVisibleForUserWithRole(
                    givenRelationship,
                    "hs_office_contact#ninthcontact.admin");
            assertThatRelationshipActuallyInDatabase(givenRelationship);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_contact#ninthcontact.admin");
                givenRelationship.setContact(null); // TODO
                return relationshipRepo.save(givenRelationship);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_relationship uuid");
        }

        private void assertThatRelationshipActuallyInDatabase(final HsOfficeRelationshipEntity saved) {
            final var found = relationshipRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved).usingRecursiveComparison().isEqualTo(saved);
        }

        private void assertThatRelationshipIsVisibleForUserWithRole(
                final HsOfficeRelationshipEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatRelationshipActuallyInDatabase(entity);
            }).assertSuccessful();
        }

        private void assertThatRelationshipIsNotVisibleForUserWithRole(
                final HsOfficeRelationshipEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                final var found = relationshipRepo.findByUuid(entity.getUuid());
                assertThat(found).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyRelationship() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenRelationship = givenSomeTemporaryRelationshipBessler(
                    "Anita", "tenth");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                relationshipRepo.deleteByUuid(givenRelationship.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return relationshipRepo.findByUuid(givenRelationship.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void contactUser_canViewButNotDeleteTheirRelatedRelationship() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenRelationship = givenSomeTemporaryRelationshipBessler(
                    "Anita", "eleventh");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("contact-admin@eleventhcontact.example.com");
                assertThat(relationshipRepo.findByUuid(givenRelationship.getUuid())).isPresent();
                relationshipRepo.deleteByUuid(givenRelationship.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office_relationship");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return relationshipRepo.findByUuid(givenRelationship.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingARelationshipAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(roleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(grantDisplaysOf(rawGrantRepo.findAll()));
            final var givenRelationship = givenSomeTemporaryRelationshipBessler(
                    "Anita", "twelfth");
            assertThat(rawRoleRepo.findAll().size()).as("unexpected number of roles created")
                    .isEqualTo(initialRoleNames.length + 3);
            assertThat(rawGrantRepo.findAll().size()).as("unexpected number of grants created")
                    .isEqualTo(initialGrantNames.length + 12);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return relationshipRepo.deleteByUuid(givenRelationship.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    private HsOfficeRelationshipEntity givenSomeTemporaryRelationshipBessler(final String holderPerson, final String contact) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike(holderPerson).get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike(contact).get(0);
            final var newRelationship = HsOfficeRelationshipEntity.builder()
                    .uuid(UUID.randomUUID())
                    .relType(HsOfficeRelationshipType.JOINT_AGENT)
                    .relAnchor(givenAnchorPerson)
                    .relHolder(givenHolderPerson)
                    .contact(givenContact)
                    .build();

            toCleanup(newRelationship);

            return relationshipRepo.save(newRelationship);
        }).assertSuccessful().returnedValue();
    }

    private HsOfficeRelationshipEntity toCleanup(final HsOfficeRelationshipEntity tempRelationship) {
        tempRelationships.add(tempRelationship);
        return tempRelationship;
    }

    @AfterEach
    void cleanup() {
        context("superuser-alex@hostsharing.net", null);
        tempRelationships.forEach(tempRelationship -> {
            System.out.println("DELETING temporary relationship: " + tempRelationship);
            relationshipRepo.deleteByUuid(tempRelationship.getUuid());
        });
    }

    void exactlyTheseRelationshipsAreReturned(
            final List<HsOfficeRelationshipEntity> actualResult,
            final String... relationshipNames) {
        assertThat(actualResult)
                .extracting(HsOfficeRelationshipEntity::toString)
                .containsExactlyInAnyOrder(relationshipNames);
    }

    void allTheseRelationshipsAreReturned(
            final List<HsOfficeRelationshipEntity> actualResult,
            final String... relationshipNames) {
        assertThat(actualResult)
                .extracting(HsOfficeRelationshipEntity::toString)
                .contains(relationshipNames);
    }
}
