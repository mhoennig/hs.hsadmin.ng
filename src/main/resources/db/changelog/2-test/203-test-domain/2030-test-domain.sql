--liquibase formatted sql

-- ============================================================================
--changeset test-domain-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists test_domain
(
    uuid        uuid unique references RbacObject (uuid),
    packageUuid uuid references test_package (uuid),
    name        character varying(253),
    description character varying(96)
);
--//
