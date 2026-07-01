--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs_accounts-account-TEST-DATA context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$

declare
    superuserAlexSubjectUuid uuid;
    personAlexUuid uuid;
    superuserFranSubjectUuid uuid;
    personFranUuid uuid;
    userDrewSubjectUuid uuid;
    personDrewUuid uuid;

begin
    call base.defineContext('creating booking-project test-data', null, 'hsh-alex_superuser', 'rbac.global#global:ADMIN');

    superuserAlexSubjectUuid = (SELECT uuid FROM rbac.subject WHERE name='hsh-alex_superuser');
    personAlexUuid = (SELECT uuid FROM hs_office.person WHERE givenName='Alex');
    superuserFranSubjectUuid = (SELECT uuid FROM rbac.subject WHERE name='hsh-fran_superuser');
    personFranUuid = (SELECT uuid FROM hs_office.person WHERE givenName='Fran');
    userDrewSubjectUuid = (SELECT uuid FROM rbac.subject WHERE name='tst-drew_selfregistered');
    personDrewUuid = (SELECT uuid FROM hs_office.person WHERE givenName='Drew');

    -- Add test account (linking to assumed rbac.subject UUIDs)
    INSERT INTO hs_accounts.account (uuid, version, person_uuid, global_uid, global_gid) VALUES
        ( superuserAlexSubjectUuid, 0, personAlexUuid, 1001, 1001),
        ( superuserFranSubjectUuid, 0, personFranUuid, 1002, 1002),
        ( userDrewSubjectUuid, 0, personDrewUuid, 1003, 1003);

end; $$;
--//
