--liquibase formatted sql


-- ============================================================================
-- UUID-OSSP-EXTENSION
--changeset michael.hoennig:uuid-ossp-extension endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Makes improved uuid generation available.
 */
create extension if not exists "uuid-ossp";
--//
