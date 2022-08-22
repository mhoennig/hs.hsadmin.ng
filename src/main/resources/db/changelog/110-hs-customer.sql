--liquibase formatted sql

-- ============================================================================
--changeset hs-customer-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists customer
(
    uuid          uuid unique references RbacObject (uuid),
    reference     int not null unique check (reference between 10000 and 99999),
    prefix        character(3) unique,
    adminUserName varchar(63)
);
--//
