--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:rbactest-SCHEMA endDelimiter:--//
-- ----------------------------------------------------------------------------
CREATE SCHEMA rbactest; -- just 'test' does not work, databasechangelog gets emptied or deleted
--//
