package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePerson;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsAccountRepository extends Repository<HsAccountEntity, UUID> {

    @Timed("app.login.account.repo.findByUuid")
    Optional<HsAccountEntity> findByUuid(final UUID uuid);

    @Timed("app.login.account.repo.findByPerson")
    List<HsAccountEntity> findByPerson(final HsOfficePerson<?> personUuid);

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
}
