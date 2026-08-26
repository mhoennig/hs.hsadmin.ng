package net.hostsharing.hsadminng.hs.office.relation;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.lambda.Reducer;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.NATURAL_PERSON;
import static net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType.UNINCORPORATED_FIRM;
import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.role.RbacRoleType.ADMIN;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@RbacDataJpaTest
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
    HsOfficeContactRbacRepository contactRbacRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    class AssumeRelationRole {

        // TODO.test: these tests would be better placed in the rbac module,
        //  but for this we need an extra long object-idname in the rbac test data

        @Test
        public void testHostsharingAdminCanAssumeRelationRoleWithLongIdName() {
            context(
                    "hsh-alex_superuser",
                    "hs_office.relation#HostsharingeG-with-PARTNER-PeterSmith-TheSecondHandandThriftStores-n-Shippinge.K.SmithPeter:AGENT");
        }

        @Test
        public void testHostsharingAdminCanAssumeRelationRoleWithUuid() {
            final var relationUuid = relationRealRepo.findRelations(HsOfficeRelationSearchCriteria.builder()
                            .relationType(HsOfficeRelationType.PARTNER)
                            .personData("%Second%")
                            .build())
                    .stream().reduce(Reducer::toSingleElement).orElseThrow().getUuid();

            context("hsh-alex_superuser", "hs_office.relation#" + relationUuid + ":AGENT");
        }
    }

    @Nested
    class CreateRelation {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewRelation() {
            // given
            context("hsh-alex_superuser");

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
                            "rel(anchor='UF Erben Bessler', type=SUBSCRIBER, mark='operations-announce', holder='NP Winkler, Paul', contact='fourth contact')");
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("hsh-alex_superuser");
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
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER to user:hsh-alex_superuser by hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER and assume }",

                    "{ grant perm:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:UPDATE to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:OWNER to role:hs_office.person#BesslerBert:ADMIN by system and assume }",
                    "{ grant role:hs_office.person#ErbenBesslerMelBessler:OWNER to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",

                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT to role:hs_office.person#ErbenBesslerMelBessler:ADMIN by system and assume }",
                    "{ grant role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:ADMIN by system and assume }",
                    "{ grant role:hs_office.contact#fourthcontact:ADMIN to role:hs_office.relation#ErbenBesslerMelBessler-with-REPRESENTATIVE-BesslerBert:AGENT by system and assume }",

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

        @Test
        public void creatingARepresentativeRelationAsAnchorPersonAdmin_doesNotCreateAGrantCycle() {
            // given
            context("hsh-alex_superuser", "hs_office.person#ErbenBesslerMelBessler:ADMIN");
            val givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                    .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                    .findFirst().orElseThrow();
            val givenHolderPerson = personRepo.findPersonByOptionalNameLike("Bert").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            val givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth contact").stream()
                    .findFirst().orElseThrow();

            // when
            val result = attempt(
                    em, () -> {
                        val newRelation = HsOfficeRelationRbacEntity.builder()
                                .anchor(givenAnchorPerson)
                                .holder(givenHolderPerson)
                                .type(HsOfficeRelationType.REPRESENTATIVE)
                                .contact(givenContact)
                                .build();
                        return toCleanup(relationRbacRepo.save(newRelation));
                    });

            // then
            result.assertSuccessful();
            val personAdminRoleUuid = roleUuidOf(givenAnchorPerson.getUuid(), "ADMIN");
            val relationOwnerRoleUuid = roleUuidOf(result.returnedValue().getUuid(), "OWNER");
            val relationAgentRoleUuid = roleUuidOf(result.returnedValue().getUuid(), "AGENT");

            // the creating context must not gain the relation OWNER: relation:ADMIN is granted the
            // anchor-person's OWNER by the spec, thus that grant would close a privilege-escalating cycle
            assertThat(isGranted(personAdminRoleUuid, relationOwnerRoleUuid)).isFalse();
            // but the creating context keeps its spec-defined AGENT access to the new relation
            assertThat(isGranted(personAdminRoleUuid, relationAgentRoleUuid)).isTrue();
        }

        @Test
        public void creatingARepresentativeRelationForAHolderPersonCreatedByTheSameContext_doesNotCloseAGrantCycle() {
            // given: the anchor-person's ADMIN creates the future holder person, thus gaining creator access to it
            context("hsh-alex_superuser", "hs_office.person#ErbenBesslerMelBessler:ADMIN");
            val givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                    .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                    .findFirst().orElseThrow();
            val givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth contact").stream()
                    .findFirst().orElseThrow();
            val newHolderPerson = toCleanup(personRepo.save(HsOfficePersonRealEntity.builder()
                    .personType(NATURAL_PERSON)
                    .givenName("Holly")
                    .familyName("Creator-Owned")
                    .build()));
            em.flush();

            // when
            val result = attempt(
                    em, () -> {
                        val newRelation = HsOfficeRelationRbacEntity.builder()
                                .anchor(givenAnchorPerson)
                                .holder(newHolderPerson)
                                .type(HsOfficeRelationType.REPRESENTATIVE)
                                .contact(givenContact)
                                .build();
                        return toCleanup(relationRbacRepo.save(newRelation));
                    });

            // then: the spec grants apply despite the creator access to the holder person ...
            result.assertSuccessful();
            val anchorAdminRoleUuid = roleUuidOf(givenAnchorPerson.getUuid(), "ADMIN");
            val holderAdminRoleUuid = roleUuidOf(newHolderPerson.getUuid(), "ADMIN");
            val relationOwnerRoleUuid = roleUuidOf(result.returnedValue().getUuid(), "OWNER");
            assertThat(isGranted(holderAdminRoleUuid, relationOwnerRoleUuid)).isTrue();

            // ... the graph stays acyclic, so the anchor ADMIN must not have escalated to its own OWNER ...
            assertThat(isGranted(anchorAdminRoleUuid, roleUuidOf(givenAnchorPerson.getUuid(), "OWNER"))).isFalse();

            // ... and the creating context keeps its creator access to the holder person via direct permissions
            assertThat(isGranted(anchorAdminRoleUuid, permissionUuidOf(newHolderPerson.getUuid(), "UPDATE"))).isTrue();
            assertThat(isGranted(anchorAdminRoleUuid, permissionUuidOf(newHolderPerson.getUuid(), "DELETE"))).isTrue();
            assertThat(isGranted(anchorAdminRoleUuid, holderAdminRoleUuid)).isFalse();
        }

        private UUID permissionUuidOf(final UUID objectUuid, final String op) {
            return (UUID) em.createNativeQuery("""
                            select p.uuid from rbac.permission p
                                where p.objectuuid = :objectUuid and p.op = cast(:op as rbac.RbacOp)
                            """)
                    .setParameter("objectUuid", objectUuid)
                    .setParameter("op", op)
                    .getSingleResult();
        }

        private UUID roleUuidOf(final UUID objectUuid, final String roleType) {
            return (UUID) em.createNativeQuery("""
                            select r.uuid from rbac.role r
                                where r.objectuuid = :objectUuid and r.roletype = cast(:roleType as rbac.RoleType)
                            """)
                    .setParameter("objectUuid", objectUuid)
                    .setParameter("roleType", roleType)
                    .getSingleResult();
        }

        private boolean isGranted(final UUID granteeRoleUuid, final UUID grantedRoleUuid) {
            return (Boolean) em.createNativeQuery(
                            "select rbac.isGranted(array[cast(:granteeId as uuid)], :grantedId)")
                    .setParameter("granteeId", granteeRoleUuid)
                    .setParameter("grantedId", grantedRoleUuid)
                    .getSingleResult();
        }

        // verifies Fix for Security-Finding A-01
        @Test
        public void personOwner_canCreateNewRelationAnchoredToItsOwnPerson() {
            // given
            context("tst-person_smithpeter");
            val givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Smith").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            val givenHolderPerson = personRepo.findPersonByOptionalNameLike("Winkler").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            val givenContact = contactRealRepo.findContactByOptionalCaptionLike("third contact").stream()
                    .findFirst().orElseThrow();

            // when
            val result = attempt(
                    em, () -> {
                        val newRelation = HsOfficeRelationRbacEntity.builder()
                                .anchor(givenAnchorPerson)
                                .holder(givenHolderPerson)
                                .type(HsOfficeRelationType.SUBSCRIBER)
                                .mark("members-announce") // an existing mark, new marks are tested in CreateRelationWithMark
                                .contact(givenContact)
                                .build();
                        return toCleanup(relationRbacRepo.save(newRelation));
                    });

            // then
            result.assertSuccessful();
        }

        // verifies Fix for Security-Finding A-01
        @Test
        public void personOwner_cannotCreateNewRelationAnchoredToForeignPerson() {
            // given
            context("tst-person_smithpeter");
            val givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                    .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                    .findFirst().orElseThrow();
            val givenHolderPerson = personRepo.findPersonByOptionalNameLike("Smith").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            val givenContact = contactRealRepo.findContactByOptionalCaptionLike("third contact").stream()
                    .findFirst().orElseThrow();

            // when
            val result = attempt(
                    em, () -> {
                        val newRelation = HsOfficeRelationRbacEntity.builder()
                                .anchor(givenAnchorPerson)
                                .holder(givenHolderPerson)
                                .type(HsOfficeRelationType.REPRESENTATIVE)
                                .contact(givenContact)
                                .build();
                        return relationRbacRepo.save(newRelation);
                    });

            // then
            result.assertExceptionWithRootCauseMessage(
                    PersistenceException.class,
                    "ERROR: [403] insert into hs_office.relation ",
                    "not allowed for current subjects {tst-person_smithpeter}");
        }

        private void assertThatRelationIsPersisted(final HsOfficeRelation saved) {
            final var found = relationRbacRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().extracting(Object::toString).isEqualTo(saved.toString());
        }
    }

    @Nested
    class CreateRelationWithMark {

        @Test
        public void normalUser_canCreateRelationWithExistingMark() {
            // given
            context("tst-person_smithpeter");

            // when
            val result = attempt(em, () -> toCleanup(relationRbacRepo.save(
                    subscriberRelationAnchoredToOwnPerson("members-announce"))));

            // then
            result.assertSuccessful();
        }

        @Test
        public void normalUser_cannotCreateRelationWithNewMark() {
            // given
            context("tst-person_smithpeter");

            // when
            val result = attempt(em, () -> relationRbacRepo.save(
                    subscriberRelationAnchoredToOwnPerson("brand-new-list")));

            // then
            result.assertExceptionWithRootCauseMessage(
                    PersistenceException.class,
                    "ERROR: [403] new mark \"brand-new-list\" for relation-type SUBSCRIBER may only be introduced by a global admin");
        }

        @Test
        public void globalAdmin_canCreateRelationWithNewMark() {
            // given
            context("hsh-alex_superuser");

            // when
            val result = attempt(em, () -> toCleanup(relationRbacRepo.save(
                    subscriberRelationAnchoredToOwnPerson("brand-new-list"))));

            // then
            result.assertSuccessful();
        }

        private HsOfficeRelationRbacEntity subscriberRelationAnchoredToOwnPerson(final String mark) {
            val givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Smith").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            val givenHolderPerson = personRepo.findPersonByOptionalNameLike("Winkler").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            val givenContact = contactRealRepo.findContactByOptionalCaptionLike("third contact").stream()
                    .findFirst().orElseThrow();
            return HsOfficeRelationRbacEntity.builder()
                    .anchor(givenAnchorPerson)
                    .holder(givenHolderPerson)
                    .type(HsOfficeRelationType.SUBSCRIBER)
                    .mark(mark)
                    .contact(givenContact)
                    .build();
        }
    }

    @Nested
    class FindRelations {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllRelationsOfArbitraryPerson() {
            // given
            context("hsh-alex_superuser");
            final var person = personRepo.findPersonByOptionalNameLike("Smith").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();

            // when
            final var result = relationRbacRepo.findRelationRelatedToPersonUuid(person.getUuid());

            // then
            allTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Hostsharing eG', type=PARTNER, holder='NP Smith, Peter', contact='sixth contact')",
                    "rel(anchor='LP Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', type=REPRESENTATIVE, holder='NP Smith, Peter', contact='second contact')",
                    "rel(anchor='IF Third OHG', type=SUBSCRIBER, mark='members-announce', holder='NP Smith, Peter', contact='third contact')");
        }

        @Test
        public void normalUser_canViewRelationsOfOwnedPersons() {
            // given:
            context("tst-person_smithpeter");
            final var person = personRepo.findPersonByOptionalNameLike("Smith").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();

            // when:
            final var result = relationRbacRepo.findRelations(HsOfficeRelationSearchCriteria.builder()
                    .personUuid(person.getUuid())
                    .build());

            // then:
            exactlyTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', type=REPRESENTATIVE, holder='NP Smith, Peter', contact='second contact')",
                    "rel(anchor='IF Third OHG', type=SUBSCRIBER, mark='members-announce', holder='NP Smith, Peter', contact='third contact')",
                    "rel(anchor='LP Hostsharing eG', type=PARTNER, holder='NP Smith, Peter', contact='sixth contact')",
                    "rel(anchor='NP Smith, Peter', type=DEBITOR, holder='NP Smith, Peter', contact='third contact')");
        }

        @Test
        public void normalUser_canViewRelationsByContactUuid() {
            // given:
            context("hsh-alex_superuser");
            final var contact = contactRealRepo.findContactByOptionalCaptionLike("third contact")
                    .stream()
                    .findFirst()
                    .orElseThrow();

            // when:
            final var result = relationRbacRepo.findRelations(HsOfficeRelationSearchCriteria.builder()
                    .contactUuid(contact.getUuid())
                    .build());

            // then:
            exactlyTheseRelationsAreReturned(
                    result,
                    "rel(anchor='IF Third OHG', type=SUBSCRIBER, mark='members-announce', holder='NP Smith, Peter', contact='third contact')",
                    "rel(anchor='NP Smith, Peter', type=DEBITOR, holder='NP Smith, Peter', contact='third contact')",
                    "rel(anchor='IF Third OHG', type=REPRESENTATIVE, holder='NP Tucker, Jack', contact='third contact')",
                    "rel(anchor='IF Third OHG', type=DEBITOR, holder='IF Third OHG', contact='third contact')",
                    "rel(anchor='LP Hostsharing eG', type=PARTNER, holder='IF Third OHG', contact='third contact')");
        }

        @Test
        public void normalUser_canViewRelationsByAnchorPersonUuid() {
            // given:
            context("hsh-alex_superuser");
            final var person = personRepo.findPersonByOptionalNameLike("First GmbH").getFirst();

            // when:
            final var result = relationRbacRepo.findRelations(HsOfficeRelationSearchCriteria.builder()
                    .anchorPersonUuid(person.getUuid())
                    .build());

            // then:
            exactlyTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP First GmbH', type=DEBITOR, holder='LP First GmbH', contact='first contact')",
                    "rel(anchor='LP First GmbH', type=REPRESENTATIVE, holder='NP Firby, Susan', contact='first contact')");
        }

        @Test
        public void normalUser_canViewRelationsByHolderPersonUuid() {
            // given:
            context("hsh-alex_superuser");
            final var person = personRepo.findPersonByOptionalNameLike("First GmbH").getFirst();

            // when:
            final var result = relationRbacRepo.findRelations(HsOfficeRelationSearchCriteria.builder()
                    .holderPersonUuid(person.getUuid())
                    .build());

            // then:
            exactlyTheseRelationsAreReturned(
                    result,
                    "rel(anchor='LP Hostsharing eG', type=PARTNER, holder='LP First GmbH', contact='first contact')",
                    "rel(anchor='LP First GmbH', type=DEBITOR, holder='LP First GmbH', contact='first contact')");
        }
    }

    @Nested
    class FindDistinctMarks {

        @Test
        public void normalUser_canFindMarksOfRelationsInvisibleToThem() {
            // given: a user who cannot see the test-data SUBSCRIBER relation via ReBAC
            context("tst-person_foulerellie");

            // when
            val result = relationRealRepo.findDistinctMarks(HsOfficeRelationType.SUBSCRIBER.name());

            // then
            assertThat(result).containsExactly(
                    new HsOfficeRelationMark(HsOfficeRelationType.SUBSCRIBER, "members-announce"));
        }

        @Test
        public void findsMarksOfGivenTypeDistinctAndSorted() {
            // given: two more SUBSCRIBER relations, one with a duplicate and one with another mark
            context("hsh-alex_superuser");
            val givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                    .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                    .findFirst().orElseThrow();
            val givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth contact").stream()
                    .findFirst().orElseThrow();
            for (val holderAndMark : Map.of("Paul", "operations-announce", "Bert", "members-announce").entrySet()) {
                val givenHolderPerson = personRepo.findPersonByOptionalNameLike(holderAndMark.getKey()).stream()
                        .filter(p -> p.getPersonType() == NATURAL_PERSON)
                        .findFirst().orElseThrow();
                toCleanup(relationRbacRepo.save(HsOfficeRelationRbacEntity.builder()
                        .anchor(givenAnchorPerson)
                        .holder(givenHolderPerson)
                        .type(HsOfficeRelationType.SUBSCRIBER)
                        .mark(holderAndMark.getValue())
                        .contact(givenContact)
                        .build()));
            }
            // the marks query below targets the real table, thus no auto-flush of the pending _rv inserts
            em.flush();

            // when
            val result = relationRealRepo.findDistinctMarks(HsOfficeRelationType.SUBSCRIBER.name());

            // then
            assertThat(result).containsExactly(
                    new HsOfficeRelationMark(HsOfficeRelationType.SUBSCRIBER, "members-announce"),
                    new HsOfficeRelationMark(HsOfficeRelationType.SUBSCRIBER, "operations-announce"));
        }

        @Test
        public void returnsEmptyListForRelationTypeWithoutAnyMarks() {
            // given
            context("hsh-alex_superuser");

            // when
            val result = relationRealRepo.findDistinctMarks(HsOfficeRelationType.REPRESENTATIVE.name());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        public void findsMarksOfAllTypesSortedByTypeThenMarkWhenRelationTypeOmitted() {
            // given: an OPERATIONS_ALERT relation with a mark, in addition to the SUBSCRIBER test-data
            context("hsh-alex_superuser");
            val givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Bessler").stream()
                    .filter(p -> p.getPersonType() == UNINCORPORATED_FIRM)
                    .findFirst().orElseThrow();
            val givenHolderPerson = personRepo.findPersonByOptionalNameLike("Paul").stream()
                    .filter(p -> p.getPersonType() == NATURAL_PERSON)
                    .findFirst().orElseThrow();
            val givenContact = contactRealRepo.findContactByOptionalCaptionLike("fourth contact").stream()
                    .findFirst().orElseThrow();
            toCleanup(relationRbacRepo.save(HsOfficeRelationRbacEntity.builder()
                    .anchor(givenAnchorPerson)
                    .holder(givenHolderPerson)
                    .type(HsOfficeRelationType.OPERATIONS_ALERT)
                    .mark("planned-downtime")
                    .contact(givenContact)
                    .build()));
            em.flush();

            // when
            val result = relationRealRepo.findDistinctMarks(null);

            // then
            assertThat(result).containsExactly(
                    new HsOfficeRelationMark(HsOfficeRelationType.OPERATIONS_ALERT, "planned-downtime"),
                    new HsOfficeRelationMark(HsOfficeRelationType.SUBSCRIBER, "members-announce"));
        }
    }

    @Nested
    class UpdateRelation {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canUpdateContactOfArbitraryRelation() {
            // given
            context("hsh-alex_superuser");
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Bert", "fifth contact");
            assertThatRelationActuallyInDatabase(givenRelation);
            assertThatRelationIsVisibleForUserWithRole(
                    givenRelation,
                    "hs_office.person#ErbenBesslerMelBessler:ADMIN");
            context("hsh-alex_superuser");
            final var givenContact = contactRealRepo.findContactByOptionalCaptionLike("sixth contact")
                    .stream()
                    .findFirst()
                    .orElseThrow();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
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
            context("hsh-alex_superuser");
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
                context("hsh-alex_superuser");
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
            context("hsh-alex_superuser");
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
                        "hsh-alex_superuser",
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
            context("hsh-alex_superuser");
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "ninth");
            assertThatRelationIsVisibleForUserWithRole(
                    givenRelation,
                    "hs_office.contact#ninthcontact:ADMIN");
            assertThatRelationActuallyInDatabase(givenRelation);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser", "hs_office.contact#ninthcontact:ADMIN");
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
                context("hsh-alex_superuser", assumedRoles);
                assertThatRelationActuallyInDatabase(entity);
            }).assertSuccessful();
        }

        private void assertThatRelationIsNotVisibleForUserWithRole(
                final HsOfficeRelation entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser", assumedRoles);
                final var found = relationRbacRepo.findByUuid(entity.getUuid());
                assertThat(found).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class UpdateRelatedContactData {

        @Test
        public void holderPersonAdmin_canUpdateContactDataOfItsRelation() {
            // given a SUBSCRIBER relation held by Bert with its own temporary contact
            final var givenRelation = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                final var newContact = toCleanup(contactRealRepo.save(
                        HsOfficeContactRealEntity.builder().caption("temp subscriber contact").build()));
                final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").getFirst();
                final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Bert").getFirst();
                return toCleanup(relationRbacRepo.save(HsOfficeRelationRbacEntity.builder()
                        .type(HsOfficeRelationType.SUBSCRIBER)
                        .mark("members-announce")
                        .anchor(givenAnchorPerson)
                        .holder(givenHolderPerson)
                        .contact(newContact)
                        .build()));
            }).assertSuccessful().returnedValue();

            // when the holder person's ADMIN updates the contact data of that relation
            final var result = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser", "hs_office.person#BesslerBert:ADMIN");
                final var contact = contactRbacRepo.findByUuid(givenRelation.getContact().getUuid()).orElseThrow();
                contact.setCaption("updated by holder person admin");
                return contactRbacRepo.save(contact);
            });

            // then
            result.assertSuccessful();
            jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                assertThat(contactRealRepo.findByUuid(givenRelation.getContact().getUuid()))
                        .map(HsOfficeContactRealEntity::getCaption)
                        .contains("updated by holder person admin");
            }).assertSuccessful();
        }

        @Test
        public void holderPersonAdmin_cannotEvenSeeUnrelatedContacts() {
            // given an unrelated contact
            final var unrelatedContactUuid = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                return contactRealRepo.findContactByOptionalCaptionLike("sixth contact").getFirst().getUuid();
            }).assertSuccessful().returnedValue();

            // when+then: it is not even visible for the holder person's ADMIN, thus not updatable either
            jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser", "hs_office.person#BesslerBert:ADMIN");
                assertThat(contactRbacRepo.findByUuid(unrelatedContactUuid)).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyRelation() {
            // given
            context("hsh-alex_superuser", null);
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "tenth");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                relationRbacRepo.deleteByUuid(givenRelation.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("hsh-fran_superuser", null);
                return relationRbacRepo.findByUuid(givenRelation.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void contactUser_canViewButNotDeleteTheirRelatedRelation() {
            // given
            context("hsh-alex_superuser", null);
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "eleventh");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("tst-contact_admin_eleventhcontact");
                assertThat(relationRbacRepo.findByUuid(givenRelation.getUuid())).isPresent();
                relationRbacRepo.deleteByUuid(givenRelation.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office.relation");
            assertThat(jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                return relationRbacRepo.findByUuid(givenRelation.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingARelationAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("hsh-alex_superuser");
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenRelation = givenSomeTemporaryRelationBessler(
                    "Anita", "twelfth");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
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
            context("hsh-alex_superuser");
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
