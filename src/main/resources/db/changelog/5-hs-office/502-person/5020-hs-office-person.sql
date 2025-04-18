--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-person-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE hs_office.PersonType AS ENUM (
    '??',   -- unknown
    'NP',   -- natural person
    'LP',   -- legal person
    'OU',   -- organizational unit
    'IF',   -- incorporated firm
    'UF',   -- unincorporated firm
    'PI');  -- public institution

CREATE CAST (character varying as hs_office.PersonType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office.person
(
    uuid           uuid unique references rbac.object (uuid) initially deferred,
    version        int not null default 0,
    personType     hs_office.PersonType not null,
    tradeName      varchar(96),
    salutation     varchar(30),
    title          varchar(20),
    givenName      varchar(48),
    familyName     varchar(48)
);


-- ============================================================================
--changeset michael.hoennig:hs-office-person-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.person');
--//
