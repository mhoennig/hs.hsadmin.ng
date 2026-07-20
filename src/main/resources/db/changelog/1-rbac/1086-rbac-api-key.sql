--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:rbac-api-key-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Stores the SHA-256 hash of the API-key of an API_KEY subject.

    The clear-text API-key is only returned once, in the response of creating the API_KEY
    subject; only its hash is persisted. Deleting (purging) the subject cascades to its
    API-key row; a soft-deleted (deactivated) subject is rejected at login by the
    authentication filter.
 */
create table rbac.api_key
(
    uuid       uuid primary key references rbac.subject (uuid) on delete cascade,
    keyHash    varchar(64) not null unique, -- lower-case hex-encoded SHA-256 of the clear-text API-key
    created_at timestamptz not null default now()
);

grant select on rbac.api_key to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//


-- ============================================================================
--changeset michael.hoennig:rbac-api-key-scope-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Stores the named endpoint-scopes of an API-key, one row per granted scope.

    An API-key without any scope rows is unrestricted (limited only by the roles granted
    to its subject). The scope names are mapped to concrete HTTP-method+path allowlists
    in the Java enum `net.hostsharing.hsadminng.config.ApiKeyScope`; unknown scope names
    never match any endpoint (fail-closed).
 */
create table rbac.api_key_scope
(
    apiKeyUuid uuid        not null references rbac.api_key (uuid) on delete cascade,
    scope      varchar(64) not null check (scope ~ '^[a-z][a-z0-9._-]*:[a-z]+$'),
    primary key (apiKeyUuid, scope)
);

grant select on rbac.api_key_scope to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//


-- ============================================================================
--changeset michael.hoennig:rbac-api-key-scope-format-CONSTRAINT endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Relaxes the scope format to also allow `*` as the endpoint-area, e.g. `*:read`.

    A follow-up changeset (not an amendment of the original table changeset) because the
    original changeset might already be applied to deployed databases.
 */
alter table rbac.api_key_scope
    drop constraint if exists api_key_scope_scope_check;
alter table rbac.api_key_scope
    drop constraint if exists api_key_scope_scope_format;
alter table rbac.api_key_scope
    add constraint api_key_scope_scope_format
        check (scope ~ '^([a-z][a-z0-9._-]*|\*):[a-z]+$');
--//


-- ============================================================================
--changeset michael.hoennig:rbac-api-key-EXPIRES-AT-COLUMN endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Adds an optional expiry timestamp to API-keys: null means the key never expires,
    otherwise the authentication filter rejects the key once expires_at has passed.
 */
alter table rbac.api_key
    add column expires_at timestamptz;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-api-key-CREATE-FUNCTION runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

-- adding a parameter to a function creates an overload, thus drop the previous signature
drop function if exists rbac.create_api_key(uuid, varchar, varchar[]);

create or replace function rbac.create_api_key(
        subjectUuid uuid, keyHash varchar,
        scopes varchar[] default array[]::varchar[], expiresAt timestamptz default null)
    returns uuid
    language plpgsql as $$
begin
    if subjectUuid is null or keyHash is null then
        return null;
    end if;

    if not rbac.hasGlobalRoleGranted(rbac.currentSubjectUuid()) then
        raise exception '[403] Subject % not allowed to create an API-key', base.currentSubject();
    end if;

    if not exists (
        select 1 from rbac.subject
            where uuid = subjectUuid
              and type = 'API_KEY'::rbac.SubjectType
              and deactivated_at is null) then
        raise exception '[400] subject % is not an active API_KEY subject', subjectUuid;
    end if;

    insert into rbac.api_key (uuid, keyHash, expires_at) values (subjectUuid, keyHash, expiresAt);
    insert into rbac.api_key_scope (apiKeyUuid, scope)
        select subjectUuid, s from unnest(scopes) as s;
    return subjectUuid;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-api-key-RENAME-PROVISIONING-SUBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    One-time data migration for databases that already provisioned the subject under an earlier name.
 */
update rbac.subject
    set name = 'hsadminng.provisioning.key'
    where type = 'API_KEY'::rbac.SubjectType
      and name in ('initial_api_key', 'provisioning.key', 'hostsharing.provisioning.key');
--//
