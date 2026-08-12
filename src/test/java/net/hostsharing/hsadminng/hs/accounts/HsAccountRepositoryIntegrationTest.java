package net.hostsharing.hsadminng.hs.accounts;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.REPRESENTATIVE;
import static net.hostsharing.hsadminng.rbac.subject.SubjectType.GROUP;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@RbacDataJpaTest
@Tag("generalIntegrationTest")
class HsAccountRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    private static final String SUPERUSER_ALEX_SUBJECT_NAME = "hsh-alex_superuser";
    private static final String SUPERUSER_FRAN_SUBJECT_NAME = "hsh-fran_superuser";
    private static final String USER_DREW_SUBJECT_NAME = "tst-drew_selfregistered";
    private static final String TEST_USER_SUBJECT_NAME = "tst-rene_selfregistered";
    public static final String FIRST_GMBH = "first-gmbh";

    @Autowired
    private RbacSubjectRepository rbacSubjectRepo;

    @Autowired
    private HsOfficePersonRealRepository personRepo;

    @Autowired
    private HsAccountRepository accountRepository;

    @Autowired
    private JpaAttempt jpaAttempt;

    // fetched UUIDs from test-data
    private RealSubjectEntity alexSubject;
    private RealSubjectEntity drewSubject;
    private RealSubjectEntity testUserSubject;
    private HsOfficePersonRealEntity drewPerson;
    private HsOfficePersonRealEntity testUserPerson;

    @BeforeEach
    void setUp() {
        alexSubject = fetchSubjectByName(SUPERUSER_ALEX_SUBJECT_NAME);
        drewSubject = fetchSubjectByName(USER_DREW_SUBJECT_NAME);
        testUserSubject = fetchSubjectByName(TEST_USER_SUBJECT_NAME);
        drewPerson = fetchPersonByGivenName("Drew");
        testUserPerson = fetchPersonByGivenName("Test");
    }

    @Test
    public void historizationIsAvailable() {
        // given
        final String nativeQuerySql = "select * from hs_accounts.account_hv";

        // when
        historicalContext(Timestamp.from(ZonedDateTime.now().minusDays(1).toInstant()));
        final var query = em.createNativeQuery(nativeQuerySql);
        final var rowsBefore = query.getResultList();

        // then
        assertThat(rowsBefore)
                .as("hs_accounts.account_hv only contain no rows for a timestamp before test data creation")
                .hasSize(0);

        // and when
        historicalContext(Timestamp.from(ZonedDateTime.now().toInstant()));
        em.createNativeQuery(nativeQuerySql);
        final var rowsAfter = query.getResultList();

        // then
        assertThat(rowsAfter)
                .as("hs_accounts.account_hv should now contain the test-data rows for the current timestamp")
                .hasSize(3);
    }

    @Test
    void representativeShouldFindOwnAndRepresentedAccountAmongTheVisibleOnes() {
        // given
        final var firstGmbHPerson = givenPerson("First GmbH");
        givenRelation(REPRESENTATIVE)
                .withAnchorPersonLike(firstGmbHPerson)
                .withHolder(drewPerson)
                .withContact("some test contact")
                .inDatabase();
        givenAccount()
                .forSubject(FIRST_GMBH)
                .forPerson(firstGmbHPerson)
                .inDatabase();

        // when
        final var foundAccount = attempt(
                em, () -> {
                    context(USER_DREW_SUBJECT_NAME);
                    return accountRepository.findAllVisible();
                })
                .assertNotNull().returnedValue();

        // then
        assertThat(foundAccount).hasSize(2)
                .map(e -> e.getSubject().getName())
                .containsExactlyInAnyOrder(
                        USER_DREW_SUBJECT_NAME,
                        FIRST_GMBH
                );
    }

    @Test
    void globalAdminShouldFindAllAccounts() {

        // when
        final var foundAccount = attempt(
                em, () -> {
                    context(SUPERUSER_FRAN_SUBJECT_NAME);
                    return accountRepository.findAllVisible();
                })
                .assertNotNull().returnedValue();

        // then
        assertThat(foundAccount)
                .map(e -> e.getSubject().getName())
                .contains(SUPERUSER_FRAN_SUBJECT_NAME, SUPERUSER_ALEX_SUBJECT_NAME, USER_DREW_SUBJECT_NAME);
    }

    @Test
    void normalUserShouldNotFindAccountOfForeignPersonInSameOrganization() {
        // given an account of a foreign person, held by a subject of the same organization
        val foreignPerson = givenPerson("Fouler");
        givenAccount()
                .forSubject("tst-foreign_repotest")
                .forPerson(foreignPerson)
                .inDatabase();

        // when
        val foundAccounts = attempt(
                em, () -> {
                    context(USER_DREW_SUBJECT_NAME);
                    return accountRepository.findAllVisible();
                })
                .assertNotNull().returnedValue();

        // then sharing an organization is no reason to see a foreign person's account
        assertThat(foundAccounts)
                .map(e -> e.getSubject().getName())
                .doesNotContain("tst-foreign_repotest");
    }

    // verifies the deactivated-account finding (PR#260 doc, Offene Punkte): a deactivated subject stays invisible to nobody
    @Test
    void representativeCanNotSeeDeactivatedAccountOfRepresentedPerson() {
        // given an active representative (Drew) of First GmbH
        val firstGmbHPerson = givenPerson("First GmbH");
        givenRelation(REPRESENTATIVE)
                .withAnchorPersonLike(firstGmbHPerson)
                .withHolder(drewPerson)
                .withContact("some test contact")
                .inDatabase();
        // and an offboarded (deactivated) account of that same person
        val offboardedAccount = givenAccount()
                .forSubject("first-gmbh_offboarded")
                .forPerson(firstGmbHPerson)
                .inDatabase();
        deactivateSubject(offboardedAccount.getUuid());

        // when the representative queries the represented person's accounts
        val byPerson = attempt(
                em, () -> {
                    context(USER_DREW_SUBJECT_NAME);
                    return accountRepository.findVisibleByPersonUuid(firstGmbHPerson.getUuid());
                })
                .assertNotNull().returnedValue();
        val byUuid = attempt(
                em, () -> {
                    context(USER_DREW_SUBJECT_NAME);
                    return accountRepository.findVisibleByUuid(offboardedAccount.getUuid());
                })
                .assertNotNull().returnedValue();

        // then the deactivated account stays invisible on every access path
        assertThat(byPerson)
                .as("the person-filter must not list a deactivated subject's account")
                .map(e -> e.getSubject().getName())
                .doesNotContain("first-gmbh_offboarded");
        assertThat(byUuid)
                .as("a deactivated subject's account must not be retrievable by uuid")
                .isEmpty();
    }

    private void deactivateSubject(final UUID subjectUuid) {
        val subject = em.find(RealSubjectEntity.class, subjectUuid);
        context(SUPERUSER_ALEX_SUBJECT_NAME); // global admin offboards the subject
        rbacSubjectRepo.upsert(
                subject.getUuid(), subject.getName(), subject.getOrganization(),
                subject.getType().name(), true);
        em.flush();
    }

    @Test
    void shouldFindByUuidUsingTestData() {
        // when
        final var foundEntityOptional = accountRepository.findByUuid(alexSubject.getUuid());

        // then
        assertThat(foundEntityOptional).isPresent()
                .map(e -> e.getSubject().getName())
                .contains(SUPERUSER_ALEX_SUBJECT_NAME);
    }

    @Test
    void shouldSaveAccount() {
        // given
        final var newAccount = HsAccountEntity.builder()
                .subject(testUserSubject)
                .person(testUserPerson)
                .globalUid(2011)
                .globalGid(2011)
                .build();

        // when
        toCleanup(accountRepository.save(newAccount));
        em.flush();
        em.clear();

        // then
        final var foundEntityOptional = accountRepository.findByUuid(testUserSubject.getUuid());
        assertThat(foundEntityOptional).isPresent();
        final var foundEntity = foundEntityOptional.get();
        assertThat(foundEntity.getVersion()).isEqualTo(0); // Initial version
        assertThat(foundEntity.getGlobalUid()).isEqualTo(2011);
    }

    @Test
    void nextFreeGlobalIdSkipsUsedNumbersInRange() {
        // given the lowest currently free number in the accounts uid/gid range
        final int base = accountRepository.findNextFreeGlobalId();
        assertThat(base).isBetween(100000, 999999);

        // when an account occupies exactly that number
        toCleanup(accountRepository.save(HsAccountEntity.builder()
                .subject(testUserSubject)
                .person(testUserPerson)
                .globalUid(base)
                .globalGid(base)
                .build()));
        em.flush();

        // then the next free number skips the now-used one
        assertThat(accountRepository.findNextFreeGlobalId()).isEqualTo(base + 1);
    }

    @Test
    void shouldRejectAccountForGroupSubject() {
        // when
        final var result = jpaAttempt.transacted(() -> {
            context(null);
            final var groupSubject = rbacSubjectRepo.create(RbacSubjectEntity.builder()
                    .name("/test-user-" + System.currentTimeMillis())
                    .type(GROUP)
                    .build());
            final var realGroupSubject = em.find(RealSubjectEntity.class, groupSubject.getUuid());

            final var newAccount = HsAccountEntity.builder()
                    .subject(realGroupSubject)
                    .person(em.find(HsOfficePersonRealEntity.class, testUserPerson.getUuid()))
                    .globalUid(2012)
                    .globalGid(2012)
                    .build();

            accountRepository.save(newAccount);
            em.flush();
        });

        // then
        result.assertExceptionWithRootCauseMessage(
                RuntimeException.class,
                "must be of type USER, but is GROUP");
    }

    private RealSubjectEntity fetchSubjectByName(final String name) {
        final String jpql = "SELECT s FROM RealSubjectEntity s WHERE s.name = :name";
        final Query query = em.createQuery(jpql, RealSubjectEntity.class);
        query.setParameter("name", name);
        try {
            context(SUPERUSER_ALEX_SUBJECT_NAME);
            return notNull((RealSubjectEntity) query.getSingleResult());
        } catch (final NoResultException e) {
            throw new AssertionError(
                    "Failed to find subject with name '" + name + "'. Ensure test data is present.", e);
        }
    }

    private HsOfficePersonRealEntity fetchPersonByGivenName(final String givenName) {
        final String jpql = "SELECT p FROM HsOfficePersonRealEntity p WHERE p.givenName = :givenName";
        final Query query = em.createQuery(jpql, HsOfficePersonRealEntity.class);
        query.setParameter("givenName", givenName);
        try {
            context(SUPERUSER_ALEX_SUBJECT_NAME);
            return notNull((HsOfficePersonRealEntity) query.getSingleResult());
        } catch (final NoResultException e) {
            throw new AssertionError(
                    "Failed to find person with name '" + givenName + "'. Ensure test data is present.", e);
        }
    }

    private <T> T notNull(final T result) {
        assertThat(result).isNotNull();
        return result;
    }

    @SafeVarargs
    private <T> Set<T> mutableSetOf(final T... elements) {
        return new HashSet<T>(Set.of(elements));
    }

    private HsOfficePersonRealEntity givenPerson(String personName) {
        return personRepo.findPersonByOptionalNameLike(personName).getFirst();
    }

    private RelationBuilder givenRelation(HsOfficeRelationType relationType) {
        return new RelationBuilder(relationType);
    }

    private AccountBuilder givenAccount() {
        return new AccountBuilder();
    }

    private class RelationBuilder {
        private final HsOfficeRelationType relationType;
        private HsOfficePersonRealEntity anchorPerson;
        private HsOfficePersonRealEntity holderPerson;
        private HsOfficeContactRealEntity contact;

        public RelationBuilder(HsOfficeRelationType relationType) {
            this.relationType = relationType;
        }

        public RelationBuilder withAnchorPersonLike(HsOfficePersonRealEntity anchorPerson) {
            this.anchorPerson = anchorPerson;
            return this;
        }

        public RelationBuilder withHolder(HsOfficePersonRealEntity holderPerson) {
            this.holderPerson = holderPerson;
            return this;
        }

        public RelationBuilder withContact(String caption) {
            this.contact = HsOfficeContactRealEntity.builder()
                    .caption(caption)
                    .build();
            return this;
        }

        public HsOfficeRelationRealEntity inDatabase() {
            em.persist(contact);

            final var relation = HsOfficeRelationRealEntity.builder()
                    .type(relationType)
                    .anchor(anchorPerson)
                    .holder(em.getReference(HsOfficePersonRealEntity.class, holderPerson.getUuid()))
                    .contact(contact)
                    .build();
            em.persist(relation);
            em.flush();
            return relation;
        }
    }

    private class AccountBuilder {
        private RealSubjectEntity subject;
        private HsOfficePersonRealEntity person;

        public AccountBuilder forSubject(String subjectName) {
            // only the RbacSubject can be created
            val rbacSubject = toCleanup(rbacSubjectRepo.create(RbacSubjectEntity.builder()
                    .name(subjectName)
                    .build()));
            // but we need the RealSubject
            this.subject = em.find(RealSubjectEntity.class, rbacSubject.getUuid());
            return this;
        }

        public AccountBuilder forPerson(HsOfficePersonRealEntity person) {
            this.person = person;
            return this;
        }

        public HsAccountEntity inDatabase() {

            final var account = HsAccountEntity.builder()
                    .uuid(subject.getUuid())
                    .subject(subject)
                    .person(em.find(HsOfficePersonRealEntity.class, person.getUuid()))
                    .build();
            em.persist(account);
            toCleanup(account);
            em.flush();
            return account;
        }
    }
}
