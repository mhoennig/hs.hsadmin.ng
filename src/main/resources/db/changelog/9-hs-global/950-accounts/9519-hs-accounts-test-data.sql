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

    context_HSADMIN_prod    hs_accounts.context;
    context_SSH_internal    hs_accounts.context;
    context_MATRIX_internal hs_accounts.context;

begin
    call base.defineContext('creating booking-project test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

    superuserAlexSubjectUuid = (SELECT uuid FROM rbac.subject WHERE name='superuser-alex@hostsharing.net');
    personAlexUuid = (SELECT uuid FROM hs_office.person WHERE givenName='Alex');
    superuserFranSubjectUuid = (SELECT uuid FROM rbac.subject WHERE name='superuser-fran@hostsharing.net');
    personFranUuid = (SELECT uuid FROM hs_office.person WHERE givenName='Fran');

    -- Add test contexts
    INSERT INTO hs_accounts.context (uuid, type, qualifier) VALUES
        ('11111111-1111-1111-1111-111111111111', 'HSADMIN', 'prod')
        RETURNING * INTO context_HSADMIN_prod;
    INSERT INTO hs_accounts.context (uuid, type, qualifier) VALUES
        ('22222222-2222-2222-2222-222222222222', 'SSH', 'internal')
           RETURNING * INTO context_SSH_internal;
    INSERT INTO hs_accounts.context (uuid, type, qualifier) VALUES
        ('33333333-3333-3333-3333-333333333333', 'MATRIX', 'internal')
           RETURNING * INTO context_MATRIX_internal;

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
    INSERT INTO hs_accounts.credentials (uuid, version, person_uuid, active, global_uid, global_gid, onboarding_token, totp_secrets, phone_password, email_address, sms_number) VALUES
        ( superuserAlexSubjectUuid, 0, personAlexUuid, true, 1001, 1001, 'token-abc', ARRAY['otp-secret-1a', 'otp-secret-1b'], 'phone-pw-1', 'alex@example.com', '111-222-3333'),
        ( superuserFranSubjectUuid, 0, personFranUuid, true, 1002, 1002, 'token-def', ARRAY['otp-secret-2'], 'phone-pw-2', 'fran@example.com', '444-555-6666');

    -- Map credentials to contexts
    INSERT INTO hs_accounts.context_mapping (credentials_uuid, context_uuid) VALUES
        (superuserAlexSubjectUuid, '11111111-1111-1111-1111-111111111111'), -- HSADMIN context
        (superuserFranSubjectUuid, '11111111-1111-1111-1111-111111111111'), -- HSADMIN context
        (superuserAlexSubjectUuid, '22222222-2222-2222-2222-222222222222'), -- SSH context
        (superuserFranSubjectUuid, '22222222-2222-2222-2222-222222222222'), -- SSH context
        (superuserAlexSubjectUuid, '33333333-3333-3333-3333-333333333333'), -- MATRIX context
        (superuserFranSubjectUuid, '33333333-3333-3333-3333-333333333333'); -- MATRIX context

end; $$;
--//
