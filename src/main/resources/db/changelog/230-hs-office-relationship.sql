--liquibase formatted sql

-- ============================================================================
--changeset hs-office-relationship-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsOfficeRelationshipType AS ENUM ('SOLE_AGENT', 'JOINT_AGENT', 'CO_OWNER', 'ACCOUNTING_CONTACT', 'TECHNICAL_CONTACT');

CREATE CAST (character varying as HsOfficeRelationshipType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office_relationship
(
    uuid                uuid unique references RbacObject (uuid) initially deferred, -- on delete cascade
    relAnchorUuid       uuid not null references hs_office_person(uuid),
    relHolderUuid       uuid not null references hs_office_person(uuid),
    contactUuid         uuid references hs_office_contact(uuid),
    relType             HsOfficeRelationshipType not null
);
--//


-- ============================================================================
--changeset hs-office-relationship-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_relationship');
--//
