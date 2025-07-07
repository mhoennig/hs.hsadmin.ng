package net.hostsharing.hsadminng.credentials;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRbacEntity;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest
@Tag("generalIntegrationTest")
@Import({ Context.class, JpaAttempt.class })
class HsCredentialsRepositoryIntegrationTest extends ContextBasedTest {

    private static final String SUPERUSER_ALEX_SUBJECT_NAME = "superuser-alex@hostsharing.net";
    private static final String USER_DREW_SUBJECT_NAME = "selfregistered-user-drew@hostsharing.org";
    private static final String TEST_USER_SUBJECT_NAME = "selfregistered-test-user@hostsharing.org";

    // HOWTO fix UnsatisfiedDependencyException with cause "No qualifying bean of type 'jakarta.servlet.http.HttpServletRequest'"
    //  This dependency comes from class net.hostsharing.hsadminng.context.Context,
    //  which is not automatically wired in a @DataJpaTest, but just in @SpringBootTest.
    //  If, e.g. for validators, the current user or assumed roles are needed, the values need to be mocked.
    @MockitoBean
    HttpServletRequest request;

    @Autowired
    private HsCredentialsRepository credentialsRepository;

    @Autowired
    private HsCredentialsContextRealRepository loginContextRealRepo;

    // fetched UUIDs from test-data
    private RbacSubjectEntity alexSubject;
    private RbacSubjectEntity drewSubject;
    private RbacSubjectEntity testUserSubject;
    private HsOfficePersonRbacEntity drewPerson;
    private HsOfficePersonRbacEntity testUserPerson;

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
        final String nativeQuerySql = "select * from hs_credentials.credentials_hv";

        // when
        historicalContext(Timestamp.from(ZonedDateTime.now().minusDays(1).toInstant()));
        final var query = em.createNativeQuery(nativeQuerySql);
        final var rowsBefore = query.getResultList();

        // then
        assertThat(rowsBefore).as("hs_credentials.credentials_hv only contain no rows for a timestamp before test data creation").hasSize(0);

        // and when
        historicalContext(Timestamp.from(ZonedDateTime.now().toInstant()));
        em.createNativeQuery(nativeQuerySql);
        final var rowsAfter = query.getResultList();

        // then
        assertThat(rowsAfter).as("hs_credentials.credentials_hv should now contain the test-data rows for the current timestamp").hasSize(2);
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
                .subject(drewSubject)
                .person(drewPerson)
                .active(true)
                .emailAddress("drew.new@example.com")
                .globalUid(2001)
                .globalGid(2001)
                .loginContexts(mutableSetOf(existingContext))
                .build();

        // when
        credentialsRepository.save(newCredentials);
        em.flush();
        em.clear();

        // then
        final var foundEntityOptional = credentialsRepository.findByUuid(drewSubject.getUuid());
        assertThat(foundEntityOptional).isPresent();
        final var foundEntity = foundEntityOptional.get();
        assertThat(foundEntity.getEmailAddress()).isEqualTo("drew.new@example.com");
        assertThat(foundEntity.isActive()).isTrue();
        assertThat(foundEntity.getVersion()).isEqualTo(0); // Initial version
        assertThat(foundEntity.getGlobalUid()).isEqualTo(2001);

        assertThat(foundEntity.getLoginContexts()).hasSize(1)
                .map(HsCredentialsContextRealEntity::toString).contains("loginContext(HSADMIN:prod)");
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

    private HsOfficePersonRbacEntity fetchPersonByGivenName(final String givenName) {
        final String jpql = "SELECT p FROM HsOfficePersonRbacEntity p WHERE p.givenName = :givenName";
        final Query query = em.createQuery(jpql, HsOfficePersonRbacEntity.class);
        query.setParameter("givenName", givenName);
        try {
            context(SUPERUSER_ALEX_SUBJECT_NAME);
            return notNull((HsOfficePersonRbacEntity) query.getSingleResult());
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
}
