--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs_accounts-credentials-TEST-DATA context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$

declare
    superuserAlexSubjectUuid uuid;
    personAlexUuid uuid;
    superuserFranSubjectUuid uuid;
    personFranUuid uuid;
    userDrewSubjectUuid uuid;
    personDrewUuid uuid;


    context_HSADMIN_prod    hs_accounts.context;
    context_SSH_internal    hs_accounts.context;
    context_SSH_external    hs_accounts.context;
    context_MATRIX_internal hs_accounts.context;
    context_MATRIX_external hs_accounts.context;

begin
    call base.defineContext('creating booking-project test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

    superuserAlexSubjectUuid = (SELECT uuid FROM rbac.subject WHERE name='superuser-alex@hostsharing.net');
    personAlexUuid = (SELECT uuid FROM hs_office.person WHERE givenName='Alex');
    superuserFranSubjectUuid = (SELECT uuid FROM rbac.subject WHERE name='superuser-fran@hostsharing.net');
    personFranUuid = (SELECT uuid FROM hs_office.person WHERE givenName='Fran');
    userDrewSubjectUuid = (SELECT uuid FROM rbac.subject WHERE name='selfregistered-user-drew@hostsharing.org');
    personDrewUuid = (SELECT uuid FROM hs_office.person WHERE givenName='Drew');

    -- Add test contexts
    INSERT INTO hs_accounts.context (uuid, type, qualifier, only_for_natural_persons, public_access) VALUES
        ('11111111-1111-1111-1111-111111111111', 'HSADMIN', 'prod', true, true)
        RETURNING * INTO context_HSADMIN_prod;
    INSERT INTO hs_accounts.context (uuid, type, qualifier, only_for_natural_persons, public_access) VALUES
        ('22222222-2222-2222-2222-222222222222', 'SSH', 'internal', true, false)
       RETURNING * INTO context_SSH_internal;
    INSERT INTO hs_accounts.context (uuid, type, qualifier, only_for_natural_persons, public_access) VALUES
        ('33333333-3333-3333-3333-333333333333', 'SSH', 'external', false, true)
       RETURNING * INTO context_SSH_external;
    INSERT INTO hs_accounts.context (uuid, type, qualifier, only_for_natural_persons, public_access) VALUES
        ('44444444-4444-4444-4444-444444444444', 'MATRIX', 'internal', true, false)
       RETURNING * INTO context_MATRIX_internal;
    INSERT INTO hs_accounts.context (uuid, type, qualifier, only_for_natural_persons, public_access) VALUES
        ('55555555-5555-5555-5555-555555555555', 'MATRIX', 'external', true, true)
       RETURNING * INTO context_MATRIX_external;
    INSERT INTO hs_accounts.context (uuid, type, qualifier, only_for_natural_persons, public_access) VALUES
        ('66666666-6666-6666-6666-666666666666', 'MASTODON', 'external', false, true);
    INSERT INTO hs_accounts.context (uuid, type, qualifier, only_for_natural_persons, public_access) VALUES
        ('77777777-7777-7777-7777-777777777777', 'BBB', 'external', false, true);

-- grant general access to public credential contexts
-- TODO_impl: RBAC rules for _rv do not yet work properly
--     call rbac.grantPermissiontoRole(
--             rbac.createPermission(context_HSADMIN_prod.uuid, 'SELECT'),
--             rbac.global_GUEST());
--     call rbac.grantPermissiontoRole(
--             rbac.createPermission(context_SSH_internal.uuid, 'SELECT'),
--             rbac.global_ADMIN());
--     call rbac.grantPermissionToRole(
--         rbac.createPermission(context_MATRIX_internal.uuid, 'SELECT'),
--         rbac.global_ADMIN());
--     call rbac.grantRoleToRole(hs_accounts.context_REFERRER(context_SSH_internal), rbac.global_ADMIN());
--     call rbac.grantRoleToRole(hs_accounts.context_REFERRER(context_MATRIX_internal), rbac.global_ADMIN());

    -- Add test credentials (linking to assumed rbac.subject UUIDs)
    INSERT INTO hs_accounts.credentials (uuid, version, person_uuid, active, global_uid, global_gid, email_address, sms_number) VALUES
        ( superuserAlexSubjectUuid, 0, personAlexUuid, true, 1001, 1001, 'alex@example.com', '111-222-3333'),
        ( superuserFranSubjectUuid, 0, personFranUuid, true, 1002, 1002, 'fran@example.com', '444-555-6666'),
        ( userDrewSubjectUuid, 0, personDrewUuid, true, 1003, 1003, 'drew@example.org', '999-888-7777');

    -- Map credentials to contexts
    INSERT INTO hs_accounts.context_mapping (credentials_uuid, context_uuid) VALUES
        (superuserAlexSubjectUuid, context_HSADMIN_prod.uuid),
        (superuserFranSubjectUuid, context_HSADMIN_prod.uuid),
        (userDrewSubjectUuid, context_HSADMIN_prod.uuid),
        (superuserAlexSubjectUuid, context_SSH_internal.uuid),
        (superuserFranSubjectUuid, context_SSH_internal.uuid),
        (userDrewSubjectUuid, context_SSH_external.uuid),
        (superuserAlexSubjectUuid, context_MATRIX_internal.uuid),
        (superuserFranSubjectUuid, context_MATRIX_internal.uuid);

end; $$;
--//
