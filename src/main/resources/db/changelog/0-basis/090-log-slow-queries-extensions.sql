--liquibase formatted sql


-- ============================================================================
-- PG-STAT-STATEMENTS-EXTENSION
--changeset pg-stat-statements-extension:1 context:pg_stat_statements endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Makes improved uuid generation available.
 */
create extension if not exists "pg_stat_statements";
--//

