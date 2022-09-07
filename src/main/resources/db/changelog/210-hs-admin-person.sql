--liquibase formatted sql

-- ============================================================================
--changeset hs-admin-person-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsAdminPersonType AS ENUM ('NATURAL', 'LEGAL', 'SOLE_REPRESENTATION', 'JOINT_REPRESENTATION');

CREATE CAST (character varying as HsAdminPersonType) WITH INOUT AS IMPLICIT;

create table if not exists hs_admin_person
(
    uuid           uuid unique references RbacObject (uuid),
    personType     HsAdminPersonType not null,
    tradeName      varchar(96),
    givenName      varchar(48),
    familyName     varchar(48)
);
--//
