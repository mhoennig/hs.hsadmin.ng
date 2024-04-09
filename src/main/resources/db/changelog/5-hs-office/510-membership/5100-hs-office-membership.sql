--liquibase formatted sql

-- ============================================================================
--changeset hs-office-membership-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsOfficeMembershipStatus AS ENUM (
    'INVALID',
    'ACTIVE',
    'CANCELLED',
    'TRANSFERRED',
    'DECEASED',
    'LIQUIDATED',
    'EXPULSED',
    'UNKNOWN'
);

CREATE CAST (character varying as HsOfficeMembershipStatus) WITH INOUT AS IMPLICIT;

create table if not exists hs_office_membership
(
    uuid                    uuid unique references RbacObject (uuid) initially deferred,
    version                 int not null default 0,
    partnerUuid             uuid not null references hs_office_partner(uuid),
    memberNumberSuffix      char(2) not null check (memberNumberSuffix::text ~ '^[0-9][0-9]$'),
    validity                daterange not null,
    status                  HsOfficeMembershipStatus not null default 'ACTIVE',
    membershipFeeBillable   boolean not null default true,

    UNIQUE(partnerUuid, memberNumberSuffix)
);
--//


-- ============================================================================
--changeset hs-office-membership-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_membership');
--//
