--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-credentials-CREDENTIALS-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_accounts.credentials
(
    uuid             uuid PRIMARY KEY references rbac.subject (uuid) initially deferred,
    version          int not null default 0,

    person_uuid      uuid not null references hs_office.person(uuid),

    active           bool,
    last_used        timestamp,
    global_uid       int unique,     -- w/o
    global_gid       int unique,     -- w/o
    onboarding_token text,           -- w/o, but can be set to null to invalidate

    totp_secrets     text[],
    phone_password   text,
    email_address    text,
    sms_number       text
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-credentials-context-CONTEXT-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_accounts.context
(
    uuid            uuid PRIMARY KEY,
    version         int not null default 0,

    type            varchar(16),
    qualifier       varchar(80),

    unique (type, qualifier)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-credentials-CONTEXT-IMMUTABLE-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION hs_accounts.prevent_context_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Updates to hs_accounts.context are not allowed.';
END;
$$ LANGUAGE plpgsql;

-- Trigger to enforce immutability
CREATE TRIGGER context_immutable_trigger
BEFORE UPDATE ON hs_accounts.context
FOR EACH ROW EXECUTE FUNCTION hs_accounts.prevent_context_update();
--//


-- ============================================================================
--changeset michael.hoennig:hs_accounts-CONTEXT-MAPPING endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_accounts.context_mapping
(
    uuid                    uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    credentials_uuid        uuid references hs_accounts.credentials(uuid) ON DELETE CASCADE,
    context_uuid            uuid references hs_accounts.context(uuid) ON DELETE RESTRICT
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-hs_accounts-JOURNALS endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_accounts.context_mapping');
call base.create_journal('hs_accounts.context');
call base.create_journal('hs_accounts.credentials');
--//


-- ============================================================================
--changeset michael.hoennig:hs_accounts-HISTORICIZATION endDelimiter:--//
-- ----------------------------------------------------------------------------
call base.tx_create_historicization('hs_accounts.context_mapping');
call base.tx_create_historicization('hs_accounts.context');
call base.tx_create_historicization('hs_accounts.credentials');
--//
