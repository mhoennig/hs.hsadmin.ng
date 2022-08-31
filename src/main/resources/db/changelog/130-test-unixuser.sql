--liquibase formatted sql

-- ============================================================================
--changeset hs-unixuser-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists test_unixuser
(
    uuid        uuid unique references RbacObject (uuid),
    packageUuid uuid references test_package (uuid),
    name        character varying(32),
    description character varying(96)
);
--//
