--liquibase formatted sql


-- ============================================================================
-- PG-STAT-STATEMENTS-EXTENSION
--changeset michael.hoennig:pg-stat-statements-extension context:pg_stat_statements endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Makes improved uuid generation available.
 */
create extension if not exists "pg_stat_statements";
--//

