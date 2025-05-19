package net.hostsharing.hsadminng.credentials;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest
@ActiveProfiles("test")
@Tag("generalIntegrationTest")
@Import({ Context.class, JpaAttempt.class })
class HsCredentialsContextRealRepositoryIntegrationTest extends ContextBasedTest {

    // existing UUIDs from test data (Liquibase changeset 310-login-credentials-test-data.sql)
    private static final UUID TEST_CONTEXT_HSADMIN_PROD_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_CONTEXT_SSH_INTERNAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEST_CONTEXT_MATRIX_INTERNAL_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String SUPERUSER_ALEX_SUBJECT_NAME = "superuser-alex@hostsharing.net";
    private static final String TEST_USER_SUBJECT_NAME = "selfregistered-test-user@hostsharing.org";

    @MockitoBean
    HttpServletRequest request;

    @Autowired
    private HsCredentialsContextRealRepository loginContextRepository;

    @Test
    public void historizationIsAvailable() {
        // given
        final String nativeQuerySql = "select * from hs_credentials.context_hv";

        // when
        historicalContext(Timestamp.from(ZonedDateTime.now().minusDays(1).toInstant()));
        final var query = em.createNativeQuery(nativeQuerySql);
        final var rowsBefore = query.getResultList();

        // then
        assertThat(rowsBefore).as("hs_credentials.context_hv only contain no rows for a timestamp before test data creation").hasSize(0);

        // and when
        historicalContext(Timestamp.from(ZonedDateTime.now().toInstant()));
        em.createNativeQuery(nativeQuerySql, Integer.class);
        final var rowsAfter = query.getResultList();

        // then
        assertThat(rowsAfter).as("hs_credentials.context_hv should now contain the test-data rows for the current timestamp").hasSize(3);
    }

    @Test
    void shouldFindAllUsingTestData() {
        context(TEST_USER_SUBJECT_NAME);

        // when
        final var allContexts = loginContextRepository.findAll();

        // then
        assertThat(allContexts)
            .isNotNull()
            .hasSizeGreaterThanOrEqualTo(3) // Expect at least the 3 from assumed test data
            .extracting(HsCredentialsContext::getUuid)
            .contains(TEST_CONTEXT_HSADMIN_PROD_UUID, TEST_CONTEXT_SSH_INTERNAL_UUID, TEST_CONTEXT_MATRIX_INTERNAL_UUID);
    }

    @Test
    void shouldFindByUuidUsingTestData() {
        context(TEST_USER_SUBJECT_NAME);

        // when
        final var foundEntityOptional = loginContextRepository.findByUuid(TEST_CONTEXT_HSADMIN_PROD_UUID);

        // then
        assertThat(foundEntityOptional).isPresent();
        assertThat(foundEntityOptional).map(Object::toString).contains("loginContext(HSADMIN:prod)");
    }

    @Test
    void shouldFindByTypeAndQualifierUsingTestData() {
        context(TEST_USER_SUBJECT_NAME);

        // when
        final var foundEntityOptional = loginContextRepository.findByTypeAndQualifier("SSH", "internal");

        // then
        assertThat(foundEntityOptional).isPresent();
        assertThat(foundEntityOptional).map(Object::toString).contains("loginContext(SSH:internal)");
    }

    @Test
    void shouldReturnEmptyOptionalWhenFindByTypeAndQualifierNotFound() {
        context(TEST_USER_SUBJECT_NAME);

        // given
        final var nonExistentQualifier = "non-existent-qualifier";

        // when
        final var foundEntityOptional = loginContextRepository.findByTypeAndQualifier(
                "HSADMIN", nonExistentQualifier);

        // then
        assertThat(foundEntityOptional).isNotPresent();
    }

    @Test
    void shouldSaveNewLoginContext() {
        context(SUPERUSER_ALEX_SUBJECT_NAME);

        // given
        final var newQualifier = "test@example.social";
        final var newType = "MASTODON";
        final var newContext = HsCredentialsContextRealEntity.builder()
                .type(newType)
                .qualifier(newQualifier)
                .build();

        // when
        final var savedEntity = loginContextRepository.save(newContext);
        em.flush();
        em.clear();

        // then
        assertThat(savedEntity).isNotNull();
        final var generatedUuid = savedEntity.getUuid();
        assertThat(generatedUuid).isNotNull(); // Verify UUID was generated

        // Fetch again using the generated UUID to confirm persistence
        context(TEST_USER_SUBJECT_NAME); // Re-set context if needed after clear
        final var foundEntityOptional = loginContextRepository.findByUuid(generatedUuid);
        assertThat(foundEntityOptional).isPresent();
        final var foundEntity = foundEntityOptional.get();
        assertThat(foundEntity.getUuid()).isEqualTo(generatedUuid);
        assertThat(foundEntity.getType()).isEqualTo(newType);
        assertThat(foundEntity.getQualifier()).isEqualTo(newQualifier);
    }

    @Test
    void shouldPreventUpdateOfExistingLoginContext() {
        context(TEST_USER_SUBJECT_NAME);

        // given an existing entity from test data
        final var entityToUpdateOptional = loginContextRepository.findByUuid(TEST_CONTEXT_MATRIX_INTERNAL_UUID);
        assertThat(entityToUpdateOptional)
            .withFailMessage("Could not find existing LoginContext with UUID %s. Ensure test data exists.",
                    TEST_CONTEXT_MATRIX_INTERNAL_UUID)
            .isPresent();
        final var entityToUpdate = entityToUpdateOptional.get();

        // when
        entityToUpdate.setQualifier("updated");
        final var exception = catchThrowable( () -> {
            loginContextRepository.save(entityToUpdate);
            em.flush();
        });

        // then
        assertThat(exception)
            .isInstanceOf(PersistenceException.class)
            .hasCauseInstanceOf(PSQLException.class)
            .hasMessageContaining("ERROR: Updates to hs_credentials.context are not allowed.");
    }
}
