--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-profile-PROFILE-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_accounts.profile
(
    uuid             uuid PRIMARY KEY references rbac.subject (uuid) initially deferred,
    version          int not null default 0,

    person_uuid      uuid not null references hs_office.person(uuid),

    active           bool,
    global_uid       int unique,     -- w/o
    global_gid       int unique,     -- w/o

    password_hash    text,
    totp_secrets     text[],
    phone_password   text,
    email_address    text,
    sms_number       text
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-profile-scope-SCOPE-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_accounts.scope
(
    uuid                        uuid PRIMARY KEY,
    version                     int not null default 0,

    type                        varchar(16),
    qualifier                   varchar(80),

    only_for_natural_persons    boolean default false,

    public_access               boolean default false,

    unique (type, qualifier)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-profile-SCOPE-IMMUTABLE-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION hs_accounts.prevent_scope_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Updates to hs_accounts.scope are not allowed.';
END;
$$ LANGUAGE plpgsql;

-- Trigger to enforce immutability
CREATE TRIGGER scope_immutable_trigger
BEFORE UPDATE ON hs_accounts.scope
FOR EACH ROW EXECUTE FUNCTION hs_accounts.prevent_scope_update();
--//


-- ============================================================================
--changeset michael.hoennig:hs_accounts-SCOPE-MAPPING endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_accounts.scope_mapping
(
    uuid                    uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_uuid        uuid references hs_accounts.profile(uuid) ON DELETE CASCADE,
    scope_uuid              uuid references hs_accounts.scope(uuid) ON DELETE RESTRICT
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-hs_accounts-JOURNALS endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_accounts.scope_mapping');
call base.create_journal('hs_accounts.scope');
call base.create_journal('hs_accounts.profile');
--//


-- ============================================================================
--changeset michael.hoennig:hs_accounts-HISTORICIZATION endDelimiter:--//
-- ----------------------------------------------------------------------------
call base.tx_create_historicization('hs_accounts.scope_mapping');
call base.tx_create_historicization('hs_accounts.scope');
call base.tx_create_historicization('hs_accounts.profile');
--//
