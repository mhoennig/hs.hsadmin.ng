--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:test-customer-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists rbactest.customer
(
    uuid          uuid unique references rbac.object (uuid),
    version      int not null default 0,
    reference     int not null unique check (reference between 10000 and 99999),
    prefix        character(3) unique,
    adminUserName varchar(63)
);
--//
