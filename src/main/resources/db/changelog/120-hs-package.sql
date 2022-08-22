--liquibase formatted sql

-- ============================================================================
--changeset hs-package-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists package
(
    uuid         uuid unique references RbacObject (uuid),
    version      int not null default 0,
    customerUuid uuid references customer (uuid),
    name         varchar(5),
    description  varchar(96)
);
--//
