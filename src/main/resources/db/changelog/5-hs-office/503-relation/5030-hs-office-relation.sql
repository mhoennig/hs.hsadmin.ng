--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-relation-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE hs_office.RelationType AS ENUM (
    'UNKNOWN',
    'PARTNER',
    'EX_PARTNER',
    'REPRESENTATIVE',
    'DEBITOR',
    'VIP_CONTACT',
    'OPERATIONS',
    'SUBSCRIBER');

CREATE CAST (character varying as hs_office.RelationType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office.relation
(
    uuid             uuid unique references rbac.object (uuid) initially deferred, -- on delete cascade
    version          int not null default 0,
    anchorUuid       uuid not null references hs_office.person(uuid),
    holderUuid       uuid not null references hs_office.person(uuid),
    contactUuid      uuid references hs_office.contact(uuid),
    type             hs_office.RelationType not null,
    mark             varchar(24)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.relation');
--//
