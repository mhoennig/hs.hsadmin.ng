--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-membership-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE hs_office.HsOfficeMembershipStatus AS ENUM (
    'INVALID',
    'ACTIVE',
    'CANCELLED',
    'TRANSFERRED',
    'DECEASED',
    'LIQUIDATED',
    'EXPULSED',
    'UNKNOWN'
);

CREATE CAST (character varying as hs_office.HsOfficeMembershipStatus) WITH INOUT AS IMPLICIT;

create table if not exists hs_office.membership
(
    uuid                    uuid unique references rbac.object (uuid) initially deferred,
    version                 int not null default 0,
    partnerUuid             uuid not null references hs_office.partner(uuid),
    memberNumberSuffix      char(2) not null check (memberNumberSuffix::text ~ '^[0-9][0-9]$'),
    validity                daterange not null,
    status                  hs_office.HsOfficeMembershipStatus not null default 'ACTIVE',
    membershipFeeBillable   boolean not null default true,

    UNIQUE(partnerUuid, memberNumberSuffix)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-membership-SINGLE-MEMBERSHIP-CHECK endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION hs_office.validate_membership_validity()
    RETURNS trigger AS $$
DECLARE
    partnerNumber int;
BEGIN
    IF EXISTS (
        SELECT 1
            FROM hs_office.membership
            WHERE partnerUuid = NEW.partnerUuid
              AND uuid <> NEW.uuid
              AND NEW.validity && validity
    ) THEN
        SELECT p.partnerNumber INTO partnerNumber
            FROM hs_office.partner AS p
            WHERE p.uuid = NEW.partnerUuid;
        RAISE EXCEPTION 'Membership validity ranges overlap for partnerUuid %, partnerNumber %', NEW.partnerUuid, partnerNumber;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_membership_validity
    BEFORE INSERT OR UPDATE ON hs_office.membership
    FOR EACH ROW
EXECUTE FUNCTION hs_office.validate_membership_validity();


--//


-- ============================================================================
--changeset michael.hoennig:hs-office-membership-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.membership');
--//
