package net.hostsharing.hsadminng.rbac.context;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, JpaAttempt.class, EntityManagerWrapper.class, StrictMapper.class })
@DirtiesContext
class ContextIntegrationTests {

    @Autowired
    private Context context;

    @MockitoBean
    @SuppressWarnings("unused") // the bean must be present, even though it's not used directly
    private HttpServletRequest request;

    @Autowired
    private JpaAttempt jpaAttempt;

    @PersistenceContext
    private EntityManager em;

    @Test
    void defineWithoutHttpServletRequestUsesCallStack() {

        context.define("superuser-alex@hostsharing.net", null);

        assertThat(context.fetchCurrentTask())
                .isEqualTo("ContextIntegrationTests.defineWithoutHttpServletRequestUsesCallStack");
    }

    @Test
    @Transactional
    void defineWithCurrentSubjectButWithoutAssumedRoles() {
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
    void defineWithoutCurrentSubjectButWithAssumedRoles() {
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
    void defineWithUnknownCurrentSubject() {
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
    void defineWithCurrentSubjectAndAssumedRoles() {
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
    public void defineContextWithCurrentSubjectAndAssumeInaccessibleRole() {
        // when
        final var result = jpaAttempt.transacted(() ->
                context.define("customer-admin@xxx.example.com", "rbactest.package#yyy00:ADMIN")
        );

        // then
        result.assertExceptionWithRootCauseMessage(
                jakarta.persistence.PersistenceException.class,
                "ERROR: [403] subject customer-admin@xxx.example.com has no permission to assume role rbactest.package#yyy00:ADMIN");
    }

    @Test
    public void hasGlobalAdminRoleIsTrueForGlobalAdminWithoutAssumedRole() {

        final var hsGlobalAdminRole = jpaAttempt.transacted(() -> {
                    // given
                    context.define("superuser-alex@hostsharing.net");

                    // when
                    return (boolean) em.createNativeQuery("select rbac.hasGlobalAdminRole()").getSingleResult();
                }
        );

        // then
        assertThat(hsGlobalAdminRole.returnedValue()).isTrue();
    }

    @Test
    public void hasGlobalAdminRoleIsTrueForGlobalAdminWithAssumedRole() {
        final var hsGlobalAdminRole = jpaAttempt.transacted(() -> {
            // given
            context.define("superuser-alex@hostsharing.net", "rbactest.package#yyy00:ADMIN");

            // when
            return (boolean) em.createNativeQuery("select rbac.hasGlobalAdminRole()").getSingleResult();
        });

        // when

        // then
        assertThat(hsGlobalAdminRole.returnedValue()).isFalse();
    }

    @Test
    public void hasGlobalAdminRoleIsFalseForNonGlobalAdminWithoutAssumedRole() {

        final var hsGlobalAdminRole = jpaAttempt.transacted(() -> {
                    // given
                    context.define("customer-admin@xxx.example.com");

                    // when
                    return (boolean) em.createNativeQuery("select rbac.hasGlobalAdminRole()").getSingleResult();
                }
        );

        // then
        assertThat(hsGlobalAdminRole.returnedValue()).isFalse();
    }
}
