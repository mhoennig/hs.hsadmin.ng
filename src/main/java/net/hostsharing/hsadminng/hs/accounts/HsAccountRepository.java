package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsAccountRepository extends Repository<HsAccountEntity, UUID> {

    // hs_accounts.account is not RBAC-restricted, thus only to be called for global admins
    @Timed("app.login.account.repo.findAll")
    List<HsAccountEntity> findAll();

    @Timed("app.login.account.repo.findByUuid")
    Optional<HsAccountEntity> findByUuid(final UUID uuid);

    // account.uuid equals subject.uuid, thus an account is visible exactly as far as its subject is
    @Timed("app.login.account.repo.findVisibleByUuid")
    @Query(nativeQuery = true, value = """
            select account.*
              from hs_accounts.account account
              join rbac.subject s on s.uuid = account.uuid
             where account.uuid = :uuid
               and """ + RealSubjectRepository.VISIBLE_SUBJECT_CONDITION)
    Optional<HsAccountEntity> findVisibleByUuid(final UUID uuid);

    @Timed("app.login.account.repo.findVisibleByPersonUuid")
    @Query(nativeQuery = true, value = """
            select account.*
              from hs_accounts.account account
              join rbac.subject s on s.uuid = account.uuid
             where account.person_uuid = :personUuid
               and """ + RealSubjectRepository.VISIBLE_SUBJECT_CONDITION)
    List<HsAccountEntity> findVisibleByPersonUuid(final UUID personUuid);

    @Timed("app.login.account.repo.findByCurrentSubject")
    @Query(nativeQuery = true, value = """
             WITH RECURSIVE
                same_person AS (
                    SELECT own_account.person_uuid
                    FROM hs_accounts.account own_account
                    WHERE own_account.uuid = rbac.currentSubjectUuid()
                ),
                represented_persons AS (
                    SELECT relation.anchorUuid person_uuid
                    FROM hs_office.relation relation
                    WHERE relation.type = 'REPRESENTATIVE'
                      AND relation.holderUuid IN (SELECT person_uuid FROM same_person)
                )
            SELECT DISTINCT account.*
            FROM hs_accounts.account account
            WHERE account.person_uuid IN (SELECT person_uuid FROM same_person)
               OR account.person_uuid IN (SELECT person_uuid FROM represented_persons)
            """)
    List<HsAccountEntity> findByCurrentSubject();

    @Timed("app.login.account.repo.save")
    HsAccountEntity save(final HsAccountEntity entity);

    @Timed("app.login.account.repo.findNextFreeGlobalId")
    @Query(nativeQuery = true, value = "SELECT hs_accounts.next_free_global_id()")
    Integer findNextFreeGlobalId();
}
