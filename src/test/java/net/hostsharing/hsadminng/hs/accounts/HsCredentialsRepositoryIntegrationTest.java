package net.hostsharing.hsadminng.hs.accounts;

import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.hibernate.TransientObjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import static net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType.REPRESENTATIVE;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest
@Tag("generalIntegrationTest")
@Import({ Context.class, JpaAttempt.class })
class HsCredentialsRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    private static final String SUPERUSER_ALEX_SUBJECT_NAME = "superuser-alex@hostsharing.net";
    private static final String SUPERUSER_FRAN_SUBJECT_NAME = "superuser-fran@hostsharing.net";
    private static final String USER_DREW_SUBJECT_NAME = "selfregistered-user-drew@hostsharing.org";
    private static final String TEST_USER_SUBJECT_NAME = "selfregistered-test-user@hostsharing.org";

    // HOWTO fix UnsatisfiedDependencyException with cause "No qualifying bean of type 'jakarta.servlet.http.HttpServletRequest'"
    //  This dependency comes from class net.hostsharing.hsadminng.context.Context,
    //  which is not automatically wired in a @DataJpaTest, but just in @SpringBootTest.
    //  If, e.g. for validators, the current user or assumed roles are needed, the values need to be mocked.
    @MockitoBean
    HttpServletRequest request;

    @Autowired
    private HsOfficePersonRealRepository personRepo;

    @Autowired
    private HsCredentialsRepository credentialsRepository;

    @Autowired
    private HsCredentialsContextRealRepository loginContextRealRepo;

    // fetched UUIDs from test-data
    private RbacSubjectEntity alexSubject;
    private RbacSubjectEntity drewSubject;
    private RbacSubjectEntity testUserSubject;
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
        final String nativeQuerySql = "select * from hs_accounts.credentials_hv";

        // when
        historicalContext(Timestamp.from(ZonedDateTime.now().minusDays(1).toInstant()));
        final var query = em.createNativeQuery(nativeQuerySql);
        final var rowsBefore = query.getResultList();

        // then
        assertThat(rowsBefore)
                .as("hs_accounts.credentials_hv only contain no rows for a timestamp before test data creation")
                .hasSize(0);

        // and when
        historicalContext(Timestamp.from(ZonedDateTime.now().toInstant()));
        em.createNativeQuery(nativeQuerySql);
        final var rowsAfter = query.getResultList();

        // then
        assertThat(rowsAfter)
                .as("hs_accounts.credentials_hv should now contain the test-data rows for the current timestamp")
                .hasSize(3);
    }

    @Test
    void representativeShouldFindOwnAndRepresentedCredentialsByCurrentSubject() {
        // given
        final var firstGmbHPerson = givenPerson("First GmbH");
        givenRelation(REPRESENTATIVE)
                .withAnchorPersonLike(firstGmbHPerson)
                .withHolder(drewPerson)
                .withContact("some test contact");
        givenCredentials()
                .forSubject("first-gmbh")
                .forPerson(firstGmbHPerson)
                .withEMailAddress("first-gmbh@example.com");

        // when
        final var foundCredentials = attempt(
                em, () -> {
                    context(USER_DREW_SUBJECT_NAME);
                    return credentialsRepository.findByCurrentSubject();
                })
                .assertNotNull().returnedValue();

        // then
        assertThat(foundCredentials).hasSize(2)
                .map(HsCredentialsEntity::getEmailAddress)
                .containsExactlyInAnyOrder("drew@example.org", "first-gmbh@example.com");
    }

    @Test
    void globalAdminShouldFindOnlyOwnCredentialsByCurrentSubject() {

        // when
        final var foundCredentials = attempt(
                em, () -> {
                    context(SUPERUSER_FRAN_SUBJECT_NAME);
                    return credentialsRepository.findByCurrentSubject();
                })
                .assertNotNull().returnedValue();

        // then
        assertThat(foundCredentials).hasSize(1)
                .map(HsCredentialsEntity::getEmailAddress)
                .containsExactlyInAnyOrder("fran@example.com");
    }

    @Test
    void shouldFindByUuidUsingTestData() {
        // when
        final var foundEntityOptional = credentialsRepository.findByUuid(alexSubject.getUuid());

        // then
        assertThat(foundEntityOptional).isPresent()
                .map(HsCredentialsEntity::getEmailAddress).contains("alex@example.com");
    }

    @Test
    void shouldSaveCredentialsWithExistingContext() {
        // given
        final var existingContext = loginContextRealRepo.findByTypeAndQualifier("HSADMIN", "prod")
                .orElseThrow();
        final var newCredentials = HsCredentialsEntity.builder()
                .subject(testUserSubject)
                .person(testUserPerson)
                .active(true)
                .emailAddress("test-user@example.com")
                .globalUid(2011)
                .globalGid(2011)
                .loginContexts(mutableSetOf(existingContext))
                .build();

        // when
        toCleanup(credentialsRepository.save(newCredentials));
        em.flush();
        em.clear();

        // then
        final var foundEntityOptional = credentialsRepository.findByUuid(testUserSubject.getUuid());
        assertThat(foundEntityOptional).isPresent();
        final var foundEntity = foundEntityOptional.get();
        assertThat(foundEntity.getEmailAddress()).isEqualTo("test-user@example.com");
        assertThat(foundEntity.isActive()).isTrue();
        assertThat(foundEntity.getVersion()).isEqualTo(0); // Initial version
        assertThat(foundEntity.getGlobalUid()).isEqualTo(2011);

        assertThat(foundEntity.getLoginContexts()).hasSize(1)
                .map(HsCredentialsContextRealEntity::toString).contains("loginContext(HSADMIN:prod:NP-ONLY:PUBLIC)");
    }

    @Test
    void shouldNotSaveCredentialsWithNewContext() {
        // given
        final var newContext = HsCredentialsContextRealEntity.builder()
                .type("MATRIX")
                .qualifier("forbidden")
                .build();
        final var newCredentials = HsCredentialsEntity.builder()
                .subject(drewSubject)
                .active(true)
                .emailAddress("drew.new@example.com")
                .globalUid(2001)
                .globalGid(2001)
                .loginContexts(mutableSetOf(newContext))
                .build();

        // when
        final var exception = catchThrowable(() -> {
            credentialsRepository.save(newCredentials);
            em.flush();
        });

        // then
        assertThat(exception).isNotNull().hasCauseInstanceOf(TransientObjectException.class);
    }

    @Test
    void shouldSaveNewCredentialsWithoutContext() {
        // given
        final var newCredentials = HsCredentialsEntity.builder()
                .subject(testUserSubject)
                .person(testUserPerson)
                .active(true)
                .emailAddress("test.user.new@example.com")
                .globalUid(20002)
                .globalGid(2002)
                .build();

        // when
        credentialsRepository.save(newCredentials);
        em.flush();
        em.clear();

        // then
        final var foundEntityOptional = credentialsRepository.findByUuid(testUserSubject.getUuid());
        assertThat(foundEntityOptional).isPresent();
        final var foundEntity = foundEntityOptional.get();
        assertThat(foundEntity.getEmailAddress()).isEqualTo("test.user.new@example.com");
        assertThat(foundEntity.isActive()).isTrue();
        assertThat(foundEntity.getGlobalUid()).isEqualTo(20002);
        assertThat(foundEntity.getGlobalGid()).isEqualTo(2002);
        assertThat(foundEntity.getLoginContexts()).isEmpty();
    }

    @Test
    void shouldUpdateExistingCredentials() {
        // given
        final var entityToUpdate = credentialsRepository.findByUuid(alexSubject.getUuid()).orElseThrow();
        final var initialVersion = entityToUpdate.getVersion();

        // when
        entityToUpdate.setActive(false);
        entityToUpdate.setEmailAddress("updated.user1@example.com");
        final var savedEntity = credentialsRepository.save(entityToUpdate);
        em.flush();
        em.clear();

        // then
        assertThat(savedEntity.getVersion()).isGreaterThan(initialVersion);
        final var updatedEntityOptional = credentialsRepository.findByUuid(alexSubject.getUuid());
        assertThat(updatedEntityOptional).isPresent();
        final var updatedEntity = updatedEntityOptional.get();
        assertThat(updatedEntity.isActive()).isFalse();
        assertThat(updatedEntity.getEmailAddress()).isEqualTo("updated.user1@example.com");
    }


    private RbacSubjectEntity fetchSubjectByName(final String name) {
        final String jpql = "SELECT s FROM RbacSubjectEntity s WHERE s.name = :name";
        final Query query = em.createQuery(jpql, RbacSubjectEntity.class);
        query.setParameter("name", name);
        try {
            context(SUPERUSER_ALEX_SUBJECT_NAME);
            return notNull((RbacSubjectEntity) query.getSingleResult());
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

    private CredentialsBuilder givenCredentials() {
        return new CredentialsBuilder();
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

        public HsOfficeRelationRealEntity withContact(String caption) {
            this.contact = HsOfficeContactRealEntity.builder()
                    .caption(caption)
                    .build();
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

    private class CredentialsBuilder {
        private RbacSubjectEntity subject;
        private HsOfficePersonRealEntity person;

        public CredentialsBuilder forSubject(String subjectName) {
            this.subject = RbacSubjectEntity.builder()
                    .name(subjectName)
                    .build();
            em.persist(subject);
            toCleanup(subject);
            return this;
        }

        public CredentialsBuilder forPerson(HsOfficePersonRealEntity person) {
            this.person = person;
            return this;
        }

        public HsCredentialsEntity withEMailAddress(String emailAddress) {

            final var credentials = HsCredentialsEntity.builder()
                    .uuid(subject.getUuid())
                    .subject(subject)
                    .person(em.find(HsOfficePersonRealEntity.class, person.getUuid()))
                    .emailAddress(emailAddress)
                    .active(true)
                    .build();
            em.persist(credentials);
            toCleanup(credentials);
            em.flush();
            return credentials;
        }
    }
}
