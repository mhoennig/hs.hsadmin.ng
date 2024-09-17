--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:test-package-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists rbactest.package
(
    uuid         uuid unique references rbac.object (uuid),
    version      int not null default 0,
    customerUuid uuid references rbactest.customer (uuid),
    name         varchar(5),
    description  varchar(96)
);
--//
