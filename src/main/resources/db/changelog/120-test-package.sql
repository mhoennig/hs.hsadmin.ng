--liquibase formatted sql

-- ============================================================================
--changeset test-package-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists test_package
(
    uuid         uuid unique references RbacObject (uuid),
    version      int not null default 0,
    customerUuid uuid references test_customer (uuid),
    name         varchar(5),
    description  varchar(96)
);
--//
