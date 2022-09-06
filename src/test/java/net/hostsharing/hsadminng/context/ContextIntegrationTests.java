package net.hostsharing.hsadminng.context;

import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, JpaAttempt.class })
@DirtiesContext
class ContextIntegrationTests {

    @Autowired
    private Context context;

    @MockBean
    private HttpServletRequest request;

    @Autowired
    private JpaAttempt jpaAttempt;

    @Test
    void defineWithoutHttpServletRequestUsesCallStack() {

        context.define("alex@hostsharing.net", null);

        assertThat(context.getCurrentTask())
                .isEqualTo("ContextIntegrationTests.defineWithoutHttpServletRequestUsesCallStack");
    }

    @Test
    @Transactional
    void defineWithCurrentUserButWithoutAssumedRoles() {
        // when
        context.define("alex@hostsharing.net");

        // then
        assertThat(context.getCurrentUser()).
                isEqualTo("alex@hostsharing.net");

        assertThat(context.getCurrentUserUUid()).isNotNull();

        assertThat(context.getAssumedRoles()).isEmpty();

        assertThat(context.currentSubjectsUuids())
                .containsExactly(context.getCurrentUserUUid());
    }

    @Test
    void defineWithoutCurrentUserButWithAssumedRoles() {
        // when
        final var result = jpaAttempt.transacted(() ->
                context.define(null, "test_package#yyy00.admin")
        );

        // then
        result.assertExceptionWithRootCauseMessage(
                javax.persistence.PersistenceException.class,
                "ERROR: [403] undefined has no permission to assume role test_package#yyy00.admin");
    }

    @Test
    void defineWithUnknownCurrentUser() {
        // when
        final var result = jpaAttempt.transacted(() ->
                context.define("unknown@example.org")
        );

        // then
        result.assertExceptionWithRootCauseMessage(
                javax.persistence.PersistenceException.class,
                "[401] user unknown@example.org given in `defineContext(...)` does not exist");
    }

    @Test
    @Transactional
    void defineWithCurrentUserAndAssumedRoles() {
        // given
        context.define("alex@hostsharing.net", "test_customer#xxx.owner;test_customer#yyy.owner");

        // when
        final var currentUser = context.getCurrentUser();
        assertThat(currentUser).isEqualTo("alex@hostsharing.net");

        // then
        assertThat(context.getAssumedRoles())
                .isEqualTo(Array.of("test_customer#xxx.owner", "test_customer#yyy.owner"));
        assertThat(context.currentSubjectsUuids()).hasSize(2);
    }

    @Test
    public void defineContextWithCurrentUserAndAssumeInaccessibleRole() {
        // when
        final var result = jpaAttempt.transacted(() ->
                context.define("customer-admin@xxx.example.com", "test_package#yyy00.admin")
        );

        // then
        result.assertExceptionWithRootCauseMessage(
                javax.persistence.PersistenceException.class,
                "ERROR: [403] user customer-admin@xxx.example.com has no permission to assume role test_package#yyy00.admin");
    }
}
