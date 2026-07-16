package net.hostsharing.hsadminng.rbac.context;

import lombok.val;
import net.hostsharing.hsadminng.errors.ForbiddenException;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.hibernate.exception.GenericJDBCException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("generalIntegrationTest")
@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
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

    private RbacGrantTestHelper rbacGranter;

    // TODO.test: remove this everywhere, should come from env
    @BeforeAll
    static void disableRyuk() {
        System.setProperty("testcontainers.ryuk.disabled", "true");
    }

    @BeforeEach
    void initTestHelpers() {
        rbacGranter = new RbacGrantTestHelper(context, em);
    }

    @Test
    void defineWithoutHttpServletRequestUsesCallStack() {

        context.define("hsh-alex_superuser", null);

        assertThat(context.fetchCurrentTask())
                .isEqualTo("ContextIntegrationTests.defineWithoutHttpServletRequestUsesCallStack");
    }

    @Test
    @Transactional
    void defineWithSubjectUuid() {
        // when
        final var subjectUuid = uuidOfSubjectName("hsh-alex_superuser");
        context.define(subjectUuid.toString());

        // then
        assertThat(context.fetchCurrentSubject()).
                isEqualTo("hsh-alex_superuser");

        assertThat(context.fetchCurrentSubjectUuid()).isNotNull();

        assertThat(context.fetchAssumedRolesNames()).isEmpty();

        assertThat(context.fetchCurrentSubjectOrAssumedRolesUuids())
                .containsExactly(subjectUuid);
    }

    @Test
    @Transactional
    void defineWithUnknownSubjectUuid() {
        // given
        final var unknownSubjectUuid = UUID.randomUUID();

        // when
        val exception = assertThrows(NoSuchElementException.class, () -> context.define(unknownSubjectUuid.toString()));

        // then
        assertThat(exception.getMessage()).isEqualTo("cannot find Subject by uuid: " + unknownSubjectUuid);
    }

    @Test
    @Transactional
    void defineWithSubjectNameWithoutAssumedRoles() {
        // when
        context.define("hsh-alex_superuser");

        // then
        assertThat(context.fetchCurrentSubject()).
                isEqualTo("hsh-alex_superuser");

        assertThat(context.fetchCurrentSubjectUuid()).isNotNull();

        assertThat(context.fetchAssumedRolesNames()).isEmpty();

        assertThat(context.fetchCurrentSubjectOrAssumedRolesUuids())
                .containsExactly(context.fetchCurrentSubjectUuid());
    }

    @Test
    @Transactional
    void defineWithUnknownSubjectName() {
        // when
        val exception = assertThrows(RuntimeException.class, () -> context.define("unknown-subject"));

        // then
        assertThat(exception.getMessage()).contains("[401] subject unknown-subject given in `base.defineContext(...)` does not exist");
        assertThrows(GenericJDBCException.class, () -> context.fetchCurrentSubject());
        assertThrows(GenericJDBCException.class, () -> context.fetchCurrentSubjectUuid());
        assertThrows(GenericJDBCException.class, () -> context.fetchAssumedRolesNames());
        assertThrows(GenericJDBCException.class, () -> context.fetchCurrentSubjectOrAssumedRolesUuids());
    }

    @Test
    @Transactional
    void contextDefineWithJwtContainingGroupsWillIncludeSynchronizedGroupsInEffectiveSubjects() {
        // given:
        // Some GROUP subjects that got created by rbac-global-TEST-GROUP-PATHS test data changeset.
        assertThat(subjectExists("/xyz-Team")).as("precondition failed").isTrue();
        assertThat(subjectExists("/xyz-Service")).as("precondition failed").isTrue();
        assertThat(subjectExists("not-synchronized-group")).as("precondition failed").isFalse();

        // when:
        givenJwtAuthentication("tst-drew_selfregistered", withGroups(
                "/xyz-Team",
                "/xyz-Service",
                "not-synchronized-group")
        );
        context.define();

        // then
        assertThat(context.fetchCurrentSubject()).
                isEqualTo("tst-drew_selfregistered");

        assertThat(context.fetchAssumedRolesNames()).isEmpty();

        assertThat(context.fetchCurrentSubjectOrAssumedRolesUuids())
                .containsExactlyInAnyOrder(
                        uuidOfSubjectName("tst-drew_selfregistered"),
                        uuidOfSubjectName("/xyz-Team"),
                        uuidOfSubjectName("/xyz-Service"));
    }

    @Test
    @Transactional
    void assumeRoles() {
        // given
        final var authentication = new UsernamePasswordAuthenticationToken("hsh-fran_superuser", null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        context.assumeRoles("rbactest.package#yyy00:ADMIN");

        // then
        assertThat(context.fetchCurrentSubject()).
                isEqualTo("hsh-fran_superuser");

        assertThat(context.fetchCurrentSubjectUuid()).isNotNull();

        assertThat(context.fetchAssumedRolesNames()).isEqualTo(Array.of("rbactest.package#yyy00:ADMIN"));

        assertThat(context.fetchCurrentSubjectOrAssumedRolesUuids())
                .containsExactly(context.fetchCurrentSubjectOrAssumedRolesUuids());
    }

    @Test
    @Transactional
    void findObjectUuidByIdNameReturnsNullForUnknownIdName() {
        assertThat(em.createNativeQuery("select rbac.findObjectUuidByIdName('rbactest.package', 'unknown')").getSingleResult())
                .isNull();
    }

    @Test
    @Transactional
    void assumedRolesGetAuthorizedViaGroupFromJwtButOnlyAssumedRolesRemainAsEffectiveSubject() {
        // given
        rbacGranter.as("rbactest.customer#xxx:OWNER")
                .grant("rbactest.customer#xxx:ADMIN")
                .to("/xyz-Service");
        rbacGranter.as("rbactest.customer#yyy:OWNER")
                .grant("rbactest.customer#yyy:ADMIN")
                .to("/xyz-Team");
        givenJwtAuthentication("tst-drew_selfregistered", withGroups(
                "/xyz-Team",
                "/xyz-Service"));

        // when
        context.assumeRoles("rbactest.package#xxx00:ADMIN;rbactest.package#yyy00:ADMIN");

        // then
        assertThat(context.fetchCurrentSubject()).
                isEqualTo("tst-drew_selfregistered");

        assertThat(context.fetchAssumedRolesNames()).isEqualTo(Array.of(
                "rbactest.package#xxx00:ADMIN",
                "rbactest.package#yyy00:ADMIN"));

        assertThat(context.fetchCurrentSubjectOrAssumedRolesUuids())
                .containsExactly(
                        uuidOfRoleName("rbactest.package#xxx00:ADMIN"),
                        uuidOfRoleName("rbactest.package#yyy00:ADMIN"))
                .doesNotContain(
                        uuidOfSubjectName("tst-drew_selfregistered"),
                        uuidOfSubjectName("/xyz-Team"),
                        uuidOfSubjectName("/xyz-Service"));
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
    void defineWithSubjectAndAssumedRoles() {
        // given
        context.define("hsh-alex_superuser", "rbactest.customer#xxx:OWNER;rbactest.customer#yyy:OWNER");

        // when
        final var currentSubject = context.fetchCurrentSubject();
        assertThat(currentSubject).isEqualTo("hsh-alex_superuser");

        // then
        assertThat(context.fetchAssumedRolesNames())
                .isEqualTo(Array.of("rbactest.customer#xxx:OWNER", "rbactest.customer#yyy:OWNER"));
        assertThat(context.fetchCurrentSubjectOrAssumedRolesUuids()).hasSize(2);
    }

    @Test
    public void defineContextWithSubjectAndAssumeInaccessibleRole() {
        // when
        final var result = jpaAttempt.transacted(() ->
                context.define("tst-customer_admin_xxx", "rbactest.package#yyy00:ADMIN")
        );

        // then
        result.assertExceptionWithRootCauseMessage(
                jakarta.persistence.PersistenceException.class,
                "ERROR: [403] subject tst-customer_admin_xxx has no permission to assume role rbactest.package#yyy00:ADMIN");
    }

    @Test
    public void hasGlobalAdminRoleIsTrueForGlobalAdminWithoutAssumedRole() {

        final var hsGlobalAdminRole = jpaAttempt.transacted(() -> {
                    // given
                    context.define("hsh-alex_superuser");

                    // when
                    return hasGlobalAdminRole();
                }
        );

        // then
        assertThat(hsGlobalAdminRole.returnedValue()).isTrue();
    }

    @Test
    public void hasGlobalAdminRoleIsFalseForGlobalAdminWithAssumedNonGlobalRole() {
        final var hasGlobalAdminRole = jpaAttempt.transacted(() -> {
            // given
            context.define("hsh-alex_superuser", "rbactest.package#yyy00:ADMIN");

            // when
            return hasGlobalAdminRole();
        });

        // when

        // then
        assertThat(hasGlobalAdminRole.returnedValue()).isFalse();
    }

    @Test
    public void hasGlobalAdminRoleIsTrueForGlobalAdminWithAssumedGlobalAdminRole() {
        final var hsGlobalAdminRole = jpaAttempt.transacted(() -> {
            // given
            context.define("hsh-alex_superuser", "rbac.global#global:ADMIN");

            // when
            return hasGlobalAdminRole();
        });

        // then
        assertThat(hsGlobalAdminRole.returnedValue()).isTrue();
    }

    @Test
    public void hasGlobalAdminRoleIsTrueForGlobalAdminWithOneOfMultipleAssumedRolesBeingGlobalAdminRole() {
        final var hsGlobalAdminRole = jpaAttempt.transacted(() -> {
            // given
            context.define("hsh-alex_superuser", "rbactest.customer#xxx:ADMIN;rbac.global#global:ADMIN");

            // when
            return hasGlobalAdminRole();
        });

        // then
        assertThat(hsGlobalAdminRole.returnedValue()).isTrue();
    }

    @Test
    public void hasGlobalAdminRoleFollowsRedefinedContextWithinSameTransaction() {
        final var hsGlobalAdminRole = jpaAttempt.transacted(() -> {
            context.define("hsh-alex_superuser");
            final var withoutAssumedRole = hasGlobalAdminRole();

            context.define("hsh-alex_superuser", "rbactest.customer#xxx:ADMIN");
            final var withAssumedCustomerRole = hasGlobalAdminRole();

            context.define("hsh-alex_superuser", "rbac.global#global:ADMIN");
            final var withAssumedGlobalAdminRole = hasGlobalAdminRole();

            return List.of(withoutAssumedRole, withAssumedCustomerRole, withAssumedGlobalAdminRole);
        });

        assertThat(hsGlobalAdminRole.returnedValue()).containsExactly(true, false, true);
    }

    @Test
    public void hasGlobalAdminRoleIsCachedInContext() {
        final var cachedHasGlobalAdminRole = jpaAttempt.transacted(() -> {
            context.define("hsh-alex_superuser", "rbactest.customer#xxx:ADMIN");
            final var withAssumedCustomerRole = cachedHasGlobalAdminRole();

            context.define("hsh-alex_superuser", "rbac.global#global:ADMIN");
            final var withAssumedGlobalAdminRole = cachedHasGlobalAdminRole();

            return List.of(withAssumedCustomerRole, withAssumedGlobalAdminRole);
        });

        assertThat(cachedHasGlobalAdminRole.returnedValue()).containsExactly("false", "true");
    }

    @Test
    public void hasGlobalAdminRoleIsFalseForNonGlobalAdminWithoutAssumedRole() {

        final var hsGlobalAdminRole = jpaAttempt.transacted(() -> {
                    // given
                    context.define("tst-customer_admin_xxx");

                    // when
                    return hasGlobalAdminRole();
                }
        );

        // then
        assertThat(hsGlobalAdminRole.returnedValue()).isFalse();
    }

    @Test
    @Transactional
    void requireGlobalAdminPassesForGlobalAdmin() {
        // given
        givenJwtAuthentication("hsh-alex_superuser", withoutGroups());

        // when / then
        assertThatCode(() -> context.requireGlobalAdmin("only a global-admin may upsert subjects"))
                .doesNotThrowAnyException();
    }

    @Test
    @Transactional
    void requireGlobalAdminThrowsForbiddenForNonGlobalAdmin() {
        // given
        givenJwtAuthentication("tst-customer_admin_xxx", withoutGroups());

        // when / then
        assertThatThrownBy(() -> context.requireGlobalAdmin("only a global-admin may upsert subjects"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("only a global-admin may upsert subjects");
    }

    private static @NonNull List<String> withoutGroups() {
        return Collections.emptyList();
    }

    private static @NonNull List<String> withGroups(String... groups) {
        return Arrays.asList(groups);
    }

    private UUID uuidOfSubjectName(final String name) {
        return UUID.fromString(em.createNativeQuery("SELECT uuid FROM rbac.subject WHERE name = :name")
                .setParameter("name", name).getSingleResult().toString());
    }

    private UUID createGroupSubject(final String name) {
        return (UUID) em.createNativeQuery("SELECT rbac.create_subject(:name, 'GROUP'::rbac.SubjectType)", UUID.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    private boolean subjectExists(final String name) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM rbac.subject WHERE name = :name")
                .setParameter("name", name).getSingleResult()).longValue() > 0;
    }

    private UUID uuidOfRoleName(final String roleName) {
        return UUID.fromString(em.createNativeQuery("SELECT rbac.findRoleId(:roleName)")
                .setParameter("roleName", roleName).getSingleResult().toString());
    }

    private void givenJwtAuthentication(final String subject, final List<String> groups) {
        final var jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", subject, "groups", groups));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private boolean hasGlobalAdminRole() {
        return (boolean) em.createNativeQuery("select rbac.hasGlobalAdminRole()").getSingleResult();
    }

    private String cachedHasGlobalAdminRole() {
        return (String) em.createNativeQuery("select current_setting('hsadminng.hasGlobalAdminRole', true)")
                .getSingleResult();
    }
}
