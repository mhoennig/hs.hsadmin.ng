--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:test-domain-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists rbactest.domain
(
    uuid        uuid unique references rbac.object (uuid),
    packageUuid uuid references rbactest.package (uuid),
    name        character varying(253),
    description character varying(96)
);
--//
