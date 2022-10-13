--liquibase formatted sql

-- ============================================================================
--changeset hs-office-person-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsOfficePersonType AS ENUM ('NATURAL', 'LEGAL', 'SOLE_REPRESENTATION', 'JOINT_REPRESENTATION');

CREATE CAST (character varying as HsOfficePersonType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office_person
(
    uuid           uuid unique references RbacObject (uuid) initially deferred,
    personType     HsOfficePersonType not null,
    tradeName      varchar(96),
    givenName      varchar(48),
    familyName     varchar(48)
);
--//


-- ============================================================================
--changeset hs-office-person-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_person');
--//
