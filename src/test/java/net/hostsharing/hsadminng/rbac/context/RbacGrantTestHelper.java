package net.hostsharing.hsadminng.rbac.context;

import jakarta.persistence.EntityManager;

import java.util.UUID;

/**
 * DSL for arranging RBAC grants in integration tests.
 */
class RbacGrantTestHelper {

    private final Context context;
    private final EntityManager em;

    RbacGrantTestHelper(final Context context, final EntityManager em) {
        this.context = context;
        this.em = em;
    }

    /**
     * Starts a grant as the role that is allowed to grant it.
     */
    GrantingRole as(final String grantedByRoleName) {
        return new GrantingRole(grantedByRoleName);
    }

    class GrantingRole {

        private final String grantedByRoleName;

        private GrantingRole(final String grantedByRoleName) {
            this.grantedByRoleName = grantedByRoleName;
        }

        /**
         * Selects the role to grant.
         */
        GrantedRole grant(final String grantedRoleName) {
            return new GrantedRole(grantedByRoleName, grantedRoleName);
        }
    }

    class GrantedRole {

        private final String grantedByRoleName;
        private final String grantedRoleName;

        private GrantedRole(final String grantedByRoleName, final String grantedRoleName) {
            this.grantedByRoleName = grantedByRoleName;
            this.grantedRoleName = grantedRoleName;
        }

        /**
         * Grants the selected role to the given subject.
         */
        void to(final String subjectName) {
            context.define("hsh-alex_superuser", "rbac.global#global:ADMIN");
            em.createNativeQuery("""
                    call rbac.grantRoleToSubjectUnchecked(
                        rbac.findRoleId(:grantedByRoleName),
                        rbac.findRoleId(:grantedRoleName),
                        :subjectUuid);
                    """)
                    .setParameter("grantedByRoleName", grantedByRoleName)
                    .setParameter("grantedRoleName", grantedRoleName)
                    .setParameter("subjectUuid", uuidOfSubjectName(subjectName))
                    .executeUpdate();
        }
    }

    private UUID uuidOfSubjectName(final String name) {
        return UUID.fromString(em.createNativeQuery("SELECT uuid FROM rbac.subject WHERE name = :name")
                .setParameter("name", name).getSingleResult().toString());
    }
}
