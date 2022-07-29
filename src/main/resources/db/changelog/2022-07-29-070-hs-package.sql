--liquibase formatted sql

-- ============================================================================
--changeset hs-package-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists package
(
    uuid         uuid unique references RbacObject (uuid),
    name         character varying(5),
    customerUuid uuid references customer (uuid)
);
--//
