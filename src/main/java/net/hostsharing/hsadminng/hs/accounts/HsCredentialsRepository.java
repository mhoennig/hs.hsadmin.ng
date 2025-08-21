package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePerson;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsCredentialsRepository extends Repository<HsCredentialsEntity, UUID> {

    @Timed("app.login.credentials.repo.findByUuid")
    Optional<HsCredentialsEntity> findByUuid(final UUID uuid);

    @Timed("app.login.credentials.repo.findByPerson")
    List<HsCredentialsEntity> findByPerson(final HsOfficePerson<?> personUuid);

    @Timed("app.login.credentials.repo.findByCurrentSubject")
    @Query(nativeQuery = true, value = """
             WITH RECURSIVE
                same_person AS (
                    SELECT own_credentials.person_uuid
                    FROM hs_accounts.credentials own_credentials
                    WHERE own_credentials.uuid = rbac.currentSubjectUuid()
                ),
                represented_persons AS (
                    SELECT relation.anchorUuid person_uuid
                    FROM hs_office.relation relation
                    WHERE relation.type = 'REPRESENTATIVE'
                      AND relation.holderUuid IN (SELECT person_uuid FROM same_person)
                )
            SELECT DISTINCT credentials.*
            FROM hs_accounts.credentials credentials
            WHERE credentials.person_uuid IN (SELECT person_uuid FROM same_person)
               OR credentials.person_uuid IN (SELECT person_uuid FROM represented_persons)
            """)
    List<HsCredentialsEntity> findByCurrentSubject();

    @Timed("app.login.credentials.repo.save")
    HsCredentialsEntity save(final HsCredentialsEntity entity);
}
