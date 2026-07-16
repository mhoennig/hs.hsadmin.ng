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
--changeset michael.hoennig:hs-accounts-ACCOUNT-EV runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

drop view if exists hs_accounts.account_ev;
create view hs_accounts.account_ev as
select account.uuid,
       account.person_uuid,
       account.global_uid,
       account.global_gid,
       subject.name as subject_name,
       person.personType as person_type,
       person.tradeName as person_trade_name,
       person.salutation as person_salutation,
       person.title as person_title,
       person.givenName as person_given_name,
       person.familyName as person_family_name
    from hs_accounts.account account
             join rbac.subject subject on subject.uuid = account.uuid
             join hs_office.person person on person.uuid = account.person_uuid;
--//


-- ============================================================================
--changeset michael.hoennig:hs-accounts-ACCOUNT-SUBJECT-FK-ON-DELETE-CASCADE endDelimiter:--//
-- ----------------------------------------------------------------------------
-- When a subject gets physically purged (DELETE /api/rbac/subjects/{uuid}?purge=true), its account
-- is deleted along with it; without the cascade the purge would fail with an FK violation for any
-- subject which has an account.
alter table hs_accounts.account
    drop constraint if exists account_uuid_fkey;
alter table hs_accounts.account
    add constraint account_uuid_fkey
        foreign key (uuid) references rbac.subject (uuid)
            on delete cascade
            deferrable initially deferred;
--//


-- ============================================================================
--changeset michael.hoennig:hs-accounts-ACCOUNT-SUBJECT-MUST-BE-USER runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

create or replace function rbac.assert_subject_type(subjectUuid uuid, expectedType rbac.SubjectType)
    returns void
    language plpgsql as $$
declare
    actualType rbac.SubjectType;
begin
    select type into actualType
        from rbac.subject
        where uuid = subjectUuid;

    if not found then
        raise exception '[400] subject % does not exist', subjectUuid;
    end if;

    if actualType is distinct from expectedType then
        raise exception '[400] subject % must be of type %, but is %', subjectUuid, expectedType, actualType;
    end if;
end; $$;

create or replace function hs_accounts.assert_account_subject_is_user_tf()
    returns trigger
    language plpgsql as $$
begin
    perform rbac.assert_subject_type(new.uuid, 'USER'::rbac.SubjectType);
    return new;
end; $$;

drop trigger if exists assert_account_subject_is_user_tg on hs_accounts.account;

create trigger assert_account_subject_is_user_tg
    before insert or update on hs_accounts.account
    for each row
execute function hs_accounts.assert_account_subject_is_user_tf();
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
