--liquibase formatted sql

-- ============================================================================
--changeset hs-office-relation-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsOfficeRelationType AS ENUM (
    'UNKNOWN',
    'PARTNER',
    'EX_PARTNER',
    'REPRESENTATIVE',
    'DEBITOR',
    'VIP_CONTACT',
    'OPERATIONS',
    'SUBSCRIBER');

CREATE CAST (character varying as HsOfficeRelationType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office_relation
(
    uuid             uuid unique references RbacObject (uuid) initially deferred, -- on delete cascade
    anchorUuid       uuid not null references hs_office_person(uuid),
    holderUuid       uuid not null references hs_office_person(uuid),
    contactUuid      uuid references hs_office_contact(uuid),
    type             HsOfficeRelationType not null,
    mark             varchar(24)
);
--//


-- ============================================================================
--changeset hs-office-relation-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_relation');
--//
