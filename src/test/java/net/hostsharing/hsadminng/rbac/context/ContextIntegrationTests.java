package net.hostsharing.hsadminng.rbac.context;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, JpaAttempt.class, StandardMapper.class })
@DirtiesContext
class ContextIntegrationTests {

    @Autowired
    private Context context;

    @MockBean
    @SuppressWarnings("unused") // the bean must be present, even though it's not used directly
    private HttpServletRequest request;

    @Autowired
    private JpaAttempt jpaAttempt;

    @Test
    void defineWithoutHttpServletRequestUsesCallStack() {

        context.define("superuser-alex@hostsharing.net", null);

        assertThat(context.fetchCurrentTask())
                .isEqualTo("ContextIntegrationTests.defineWithoutHttpServletRequestUsesCallStack");
    }

    @Test
    @Transactional
    void defineWithcurrentSubjectButWithoutAssumedRoles() {
        // when
        context.define("superuser-alex@hostsharing.net");

        // then
        assertThat(context.fetchCurrentSubject()).
                isEqualTo("superuser-alex@hostsharing.net");

        assertThat(context.fetchCurrentSubjectUuid()).isNotNull();

        assertThat(context.fetchAssumedRoles()).isEmpty();

        assertThat(context.fetchCurrentSubjectOrAssumedRolesUuids())
                .containsExactly(context.fetchCurrentSubjectUuid());
    }

    @Test
    void defineWithoutcurrentSubjectButWithAssumedRoles() {
        // when
        final var result = jpaAttempt.transacted(() ->
                context.define(null, "rbactest.package#yyy00:ADMIN")
        );

        // then
        result.assertExceptionWithRootCauseMessage(
                jakarta.persistence.PersistenceException.class,
                "ERROR: [403] undefined has no permission to assume role rbactest.package#yyy00:ADMIN");
    }

    @Test
    void defineWithUnknowncurrentSubject() {
        // when
        final var result = jpaAttempt.transacted(() ->
                context.define("unknown@example.org")
        );

        // then
        result.assertExceptionWithRootCauseMessage(
                jakarta.persistence.PersistenceException.class,
                "[401] subject unknown@example.org given in `base.defineContext(...)` does not exist");
    }

    @Test
    @Transactional
    void defineWithcurrentSubjectAndAssumedRoles() {
        // given
        context.define("superuser-alex@hostsharing.net", "rbactest.customer#xxx:OWNER;rbactest.customer#yyy:OWNER");

        // when
        final var currentSubject = context.fetchCurrentSubject();
        assertThat(currentSubject).isEqualTo("superuser-alex@hostsharing.net");

        // then
        assertThat(context.fetchAssumedRoles())
                .isEqualTo(Array.of("rbactest.customer#xxx:OWNER", "rbactest.customer#yyy:OWNER"));
        assertThat(context.fetchCurrentSubjectOrAssumedRolesUuids()).hasSize(2);
    }

    @Test
    public void defineContextWithcurrentSubjectAndAssumeInaccessibleRole() {
        // when
        final var result = jpaAttempt.transacted(() ->
                context.define("customer-admin@xxx.example.com", "rbactest.package#yyy00:ADMIN")
        );

        // then
        result.assertExceptionWithRootCauseMessage(
                jakarta.persistence.PersistenceException.class,
                "ERROR: [403] subject customer-admin@xxx.example.com has no permission to assume role rbactest.package#yyy00:ADMIN");
    }
}
