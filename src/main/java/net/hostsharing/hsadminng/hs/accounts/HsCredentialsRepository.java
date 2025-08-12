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
            WITH RECURSIVE owned_persons AS (
                -- Start with the person linked to current subject's credentials
                SELECT p.uuid AS person_uuid
                FROM hs_accounts.credentials c
                JOIN hs_office.person p ON p.uuid = c.person_uuid
                WHERE c.uuid = rbac.currentSubjectUuid()
                
                UNION
                
                -- Add persons where the current person has OWNER role
                SELECT p.uuid AS person_uuid
                FROM owned_persons op
                CROSS JOIN hs_office.person p
                WHERE rbac.isGranted(
                    rbac.currentSubjectUuid(),
                    rbac.findRoleId(
                        rbac.roleDescriptorOf('hs_office.person', p.uuid, 'OWNER'::rbac.RoleType, false)
                    )
                )
            )
            SELECT DISTINCT c.*
            FROM hs_accounts.credentials c
            WHERE c.uuid = rbac.currentSubjectUuid()  -- Include current subject's own credentials
               OR c.person_uuid IN (SELECT person_uuid FROM owned_persons)  -- Include credentials of owned persons
            """)
    List<HsCredentialsEntity> findByCurrentSubject();

    @Timed("app.login.credentials.repo.save")
    HsCredentialsEntity save(final HsCredentialsEntity entity);
}
