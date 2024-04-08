--liquibase formatted sql

-- ============================================================================
--changeset test-customer-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists test_customer
(
    uuid          uuid unique references RbacObject (uuid),
    version      int not null default 0,
    reference     int not null unique check (reference between 10000 and 99999),
    prefix        character(3) unique,
    adminUserName varchar(63)
);
--//
