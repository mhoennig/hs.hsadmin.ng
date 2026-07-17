package net.hostsharing.hsadminng.rbac.subject;

import lombok.val;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.subject.SubjectType.GROUP;
import static net.hostsharing.hsadminng.rbac.subject.SubjectType.USER;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(Context.class)
@Tag("generalIntegrationTest")
class RealSubjectRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    RealSubjectRepository realSubjectRepository;

    @PersistenceContext
    EntityManager em;

    @MockitoBean
    HttpServletRequest request;

    @Nested
    class FindVisibleSubjectsByOptionalNameLikeOrganizationAndType {

        @Test
        void returnsSubjectsFromCurrentSubjectsRealmPrefix() {
            context.define("tst-customer_admin_xxx");

            val result = realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(null, null, null);

            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .contains(
                            "tst-customer_admin_xxx",
                            "tst-customer_admin_yyy",
                            "tst-customer_admin_zzz")
                    .doesNotContain(
                            "hsh-alex_superuser",
                            "/hsh-Hostmasters",
                            "/xyz-Service");
        }

        @Test
        void canFilterGroupsByCurrentSubjectsRealmPrefix() {
            val uniquePart = "visibility_" + System.currentTimeMillis();
            val currentSubject = "xyz-" + uniquePart + "_actor";
            val sameRealmGroup = "/xyz-" + uniquePart + "-admins";
            val otherRealmGroup = "/abc-" + uniquePart + "-admins";
            context.define("hsh-alex_superuser");
            createSubject(currentSubject, USER);
            createSubject(sameRealmGroup, GROUP);
            createSubject(otherRealmGroup, GROUP);
            context.define(currentSubject);

            val result = realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(null, null, GROUP);

            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .contains("/xyz-Service", "/xyz-Team", sameRealmGroup)
                    .doesNotContain("/hsh-Hostmasters", otherRealmGroup);
        }

        @Test
        void visibilityFollowsExplicitOrganizationInsteadOfNamePrefix() {
            val uniquePart = "org_visibility_" + System.currentTimeMillis();
            val currentSubject = "xyz-" + uniquePart + "_actor";
            val userWithOtherPrefixButSameOrganization = "abc-" + uniquePart + "_member";
            context.define("hsh-alex_superuser");
            createSubject(currentSubject, USER);
            createSubjectWithOrganization(userWithOtherPrefixButSameOrganization, "xyz", USER);
            context.define(currentSubject);

            val result = realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(null, null, USER);

            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .contains(userWithOtherPrefixButSameOrganization);
        }

        @Test
        void canFilterByOrganization() {
            val uniquePart = "org_filter_" + System.currentTimeMillis();
            val userWithDerivedOrganization = "xyz-" + uniquePart + "_derived";
            val userWithExplicitOrganization = uniquePart + "_explicit";
            val userOfOtherOrganization = "abc-" + uniquePart + "_other";
            context.define("hsh-alex_superuser");
            createSubject(userWithDerivedOrganization, USER);
            createSubjectWithOrganization(userWithExplicitOrganization, "xyz", USER);
            createSubject(userOfOtherOrganization, USER);

            val result = realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(
                    null, "xyz", null);

            // then both derived and explicit organizations match, other organizations don't
            assertThat(result)
                    .extracting(RealSubjectEntity::getOrganization)
                    .containsOnly("xyz");
            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .contains(userWithDerivedOrganization, userWithExplicitOrganization)
                    .doesNotContain(userOfOtherOrganization);
        }

        @Test
        void doesNotReturnCrossRealmJwtGroupSubjects() {
            // JWT groups always belong to the current subject's own realm;
            // a (hypothetical) cross-realm group claim must not widen visibility
            contextWithGroups("/xyz-Team;/xyz-Service");

            val result = realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(null, null, GROUP);

            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .doesNotContain("/xyz-Team", "/xyz-Service");
        }

        @Test
        void returnsAllSubjectsForGlobalAdmin() {
            context.define("hsh-alex_superuser");

            val result = realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(null, null, null);

            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .contains(
                            "hsh-alex_superuser",
                            "tst-customer_admin_xxx",
                            "/hsh-Hostmasters",
                            "/xyz-Service");
        }

        @Test
        void returnsAllSubjectsForAssumedGlobalAdminRole() {
            context.define(
                    "RealSubjectRepositoryIntegrationTest",
                    null,
                    "hsh-alex_superuser",
                    "rbac.global#global:ADMIN",
                    null);

            val result = realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(null, null, null);

            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .contains(
                            "hsh-alex_superuser",
                            "tst-customer_admin_xxx",
                            "/hsh-Hostmasters",
                            "/xyz-Service");
        }

        @Test
        void returnsNothingForAssumedNonGlobalRole() {
            context.define(
                    "RealSubjectRepositoryIntegrationTest",
                    null,
                    "hsh-fran_superuser",
                    "rbactest.package#xxx00:ADMIN",
                    null);

            val result = realSubjectRepository.findVisibleSubjectsByOptionalNameLikeOrganizationAndType(null, null, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindEffectiveSubjectGroups {

        @Test
        void returnsJwtGroupSubjectsWithoutAssumedRoles() {
            contextWithGroups("/xyz-Team;/xyz-Service");

            final var result = realSubjectRepository.findEffectiveSubjectGroups();

            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .containsExactly(
                            "/xyz-Service",
                            "/xyz-Team");
        }

        @Test
        void returnsNoJwtGroupSubjectsWithAssumedRoles() {
            context.define(
                    "RealSubjectRepositoryIntegrationTest",
                    null,
                    "hsh-fran_superuser",
                    "rbactest.package#xxx00:ADMIN;rbactest.package#yyy00:ADMIN",
                    "/xyz-Team;/xyz-Service");

            final var result = realSubjectRepository.findEffectiveSubjectGroups();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindVisibleSubjectByUuid {

        @Test
        void returnsSameRealmSubject() {
            context.define("tst-customer_admin_xxx");

            val result = realSubjectRepository.findVisibleSubjectByUuid(uuidOf("tst-customer_admin_yyy"));

            assertThat(result).isPresent()
                    .map(RealSubjectEntity::getName).contains("tst-customer_admin_yyy");
        }

        @Test
        void returnsEmptyForSubjectOfAnotherRealm() {
            context.define("tst-customer_admin_xxx");

            val result = realSubjectRepository.findVisibleSubjectByUuid(uuidOf("hsh-alex_superuser"));

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyForCrossRealmJwtGroupSubject() {
            contextWithGroups("/xyz-Service");

            val result = realSubjectRepository.findVisibleSubjectByUuid(uuidOf("/xyz-Service"));

            assertThat(result).isEmpty();
        }

        @Test
        void returnsAnySubjectForGlobalAdmin() {
            context.define("hsh-alex_superuser");

            val result = realSubjectRepository.findVisibleSubjectByUuid(uuidOf("tst-customer_admin_xxx"));

            assertThat(result).isPresent()
                    .map(RealSubjectEntity::getName).contains("tst-customer_admin_xxx");
        }

        @Test
        void returnsEmptyForAssumedNonGlobalRole() {
            context.define(
                    "RealSubjectRepositoryIntegrationTest",
                    null,
                    "tst-customer_admin_xxx",
                    "rbactest.package#xxx00:ADMIN",
                    null);

            val result = realSubjectRepository.findVisibleSubjectByUuid(uuidOf("tst-customer_admin_yyy"));

            assertThat(result).isEmpty();
        }
    }

    private UUID uuidOf(final String subjectName) {
        return (UUID) em.createNativeQuery("select uuid from rbac.subject where name = :name")
                .setParameter("name", subjectName)
                .getSingleResult();
    }

    private void contextWithGroups(final String currentSubjectGroups) {
        context.define(
                "RealSubjectRepositoryIntegrationTest",
                null,
                "tst-person_firbysusan",
                null,
                currentSubjectGroups);
    }

    private void createSubject(final String subjectName, final SubjectType subjectType) {
        em.createNativeQuery("select rbac.create_subject(:name, cast(:type as rbac.SubjectType))")
                .setParameter("name", subjectName)
                .setParameter("type", subjectType.name())
                .getSingleResult();
    }

    private void createSubjectWithOrganization(
            final String subjectName,
            final String organization,
            final SubjectType subjectType) {
        em.createNativeQuery("select rbac.upsert_subject(:uuid, :name, :organization, cast(:type as rbac.SubjectType))")
                .setParameter("uuid", UUID.randomUUID())
                .setParameter("name", subjectName)
                .setParameter("organization", organization)
                .setParameter("type", subjectType.name())
                .getSingleResult();
    }
}
