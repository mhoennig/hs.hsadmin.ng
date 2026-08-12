package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsAccountRepository extends Repository<HsAccountEntity, UUID> {

    /**
     * The single visibility rule of all account lookups, so that every access path exposes exactly
     * the same accounts.
     *
     * <p>Deliberately without an organization branch: an account is no RBAC object, and the
     * organization-wide *subject* visibility exists for granting roles to colleagues, which needs
     * the subject but not its account with person-link and global uid/gid. Sharing a subject-name
     * prefix is thus no reason to expose a foreign person's account.</p>
     */
    // deactivated (soft-deleted) subjects are visible to nobody, not even global admins (see PR#260 doc)
    String VISIBLE_ACCOUNT_CONDITION = """
            (
                account.uuid in (select activeSubject.uuid
                                   from rbac.subject activeSubject
                                  where activeSubject.deactivated_at is null)
                and (
                    rbac.hasGlobalAdminRole()
                    or account.person_uuid in (
                        select ownAccount.person_uuid
                          from hs_accounts.account ownAccount
                         where ownAccount.uuid = rbac.currentSubjectUuid())
                    or account.person_uuid in (
                        -- the base table is safe here, because the filter cannot reach beyond the own person
                        select representation.anchorUuid
                          from hs_office.relation representation
                         where representation.type = 'REPRESENTATIVE'
                           and representation.holderUuid in (
                               select ownAccount.person_uuid
                                 from hs_accounts.account ownAccount
                                where ownAccount.uuid = rbac.currentSubjectUuid()))
                    or (    account.uuid in (select visibleSubject.uuid from rbac.subject_rv visibleSubject)
                        and account.person_uuid in (select visiblePerson.uuid from hs_office.person_rv visiblePerson))
                )
            )
            """;

    // hs_accounts.account is not RBAC-restricted, thus only to be called for global admins
    @Timed("app.login.account.repo.findAll")
    List<HsAccountEntity> findAll();

    @Timed("app.login.account.repo.findByUuid")
    Optional<HsAccountEntity> findByUuid(final UUID uuid);

    @Timed("app.login.account.repo.findVisibleByUuid")
    @Query(nativeQuery = true, value = """
            select account.*
              from hs_accounts.account account
             where account.uuid = :uuid
               and """ + VISIBLE_ACCOUNT_CONDITION)
    Optional<HsAccountEntity> findVisibleByUuid(final UUID uuid);

    @Timed("app.login.account.repo.findVisibleByPersonUuid")
    @Query(nativeQuery = true, value = """
            select account.*
              from hs_accounts.account account
             where account.person_uuid = :personUuid
               and """ + VISIBLE_ACCOUNT_CONDITION)
    List<HsAccountEntity> findVisibleByPersonUuid(final UUID personUuid);

    @Timed("app.login.account.repo.findAllVisible")
    @Query(nativeQuery = true, value = """
            select account.*
              from hs_accounts.account account
             where """ + VISIBLE_ACCOUNT_CONDITION)
    List<HsAccountEntity> findAllVisible();

    @Timed("app.login.account.repo.save")
    HsAccountEntity save(final HsAccountEntity entity);

    @Timed("app.login.account.repo.findNextFreeGlobalId")
    @Query(nativeQuery = true, value = "SELECT hs_accounts.next_free_global_id()")
    Integer findNextFreeGlobalId();
}
