--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:rbac-subject-sync-report-TABLE runOnChange:false endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------
/*
    Stores one row per finished subject-sync run, posted by the syncing API-key at the end
    of each run, so that monitoring can verify (via GET /api/rbac/subjects/last-sync) that the
    sync is still running. reported_by keeps the API-key subject-name as plain text (not a FK,
    the subject may be deleted); statistics holds the free-form report body as posted.
 */
create table rbac.subject_sync_report
(
    uuid        uuid primary key default gen_random_uuid(),
    reported_at timestamptz not null default now(),
    reported_by varchar(63) not null,
    statistics  jsonb
);

create index on rbac.subject_sync_report (reported_by, reported_at);

-- variable expansion must not go into non-repeatable changesets, thus the next changeset is required to fix this issue
grant select, insert, delete on rbac.subject_sync_report to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//


-- ============================================================================
--changeset michael.hoennig:rbac-subject-sync-report-GRANTS runOnChange:true endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Re-issues the grant from the table changeset: the grantee name differs between
    environments, so after restoring a dump from another environment it must be
    re-applied for the local restricted user.
 */
grant select, insert, delete on rbac.subject_sync_report to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//
