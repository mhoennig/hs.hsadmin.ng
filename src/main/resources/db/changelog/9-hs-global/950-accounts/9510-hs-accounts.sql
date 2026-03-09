--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-accounts-ACCOUNT-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_accounts.account
(
    uuid             uuid PRIMARY KEY references rbac.subject (uuid) initially deferred,
    version          int not null default 0,

    person_uuid      uuid not null references hs_office.person(uuid),

    global_uid       int unique,     -- w/o
    global_gid       int unique      -- w/o
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-hs_accounts-JOURNALS endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_accounts.account');
--//


-- ============================================================================
--changeset michael.hoennig:hs_accounts-HISTORICIZATION endDelimiter:--//
-- ----------------------------------------------------------------------------
call base.tx_create_historicization('hs_accounts.account');
--//
