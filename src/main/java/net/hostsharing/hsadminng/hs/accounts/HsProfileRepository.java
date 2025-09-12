package net.hostsharing.hsadminng.hs.accounts;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePerson;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HsProfileRepository extends Repository<HsProfileEntity, UUID> {

    @Timed("app.login.profile.repo.findByUuid")
    Optional<HsProfileEntity> findByUuid(final UUID uuid);

    @Timed("app.login.profile.repo.findByPerson")
    List<HsProfileEntity> findByPerson(final HsOfficePerson<?> personUuid);

    @Timed("app.login.profile.repo.findByCurrentSubject")
    @Query(nativeQuery = true, value = """
             WITH RECURSIVE
                same_person AS (
                    SELECT own_profile.person_uuid
                    FROM hs_accounts.profile own_profile
                    WHERE own_profile.uuid = rbac.currentSubjectUuid()
                ),
                represented_persons AS (
                    SELECT relation.anchorUuid person_uuid
                    FROM hs_office.relation relation
                    WHERE relation.type = 'REPRESENTATIVE'
                      AND relation.holderUuid IN (SELECT person_uuid FROM same_person)
                )
            SELECT DISTINCT profile.*
            FROM hs_accounts.profile profile
            WHERE profile.person_uuid IN (SELECT person_uuid FROM same_person)
               OR profile.person_uuid IN (SELECT person_uuid FROM represented_persons)
            """)
    List<HsProfileEntity> findByCurrentSubject();

    @Timed("app.login.profile.repo.save")
    HsProfileEntity save(final HsProfileEntity entity);
}
