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
    'OPERATIONS_ALERT',
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
--changeset michael.hoennig:hs-office-relation-unique-constraints endDelimiter:--//
--validCheckSum: 9:79e93a47a62e44c661cd8d414626e49d
-- ----------------------------------------------------------------------------

CREATE UNIQUE INDEX unique_relation_with_mark
    ON hs_office.relation (type, anchorUuid, holderUuid, contactUuid, mark)
    WHERE mark IS NOT NULL;

CREATE UNIQUE INDEX unique_relation_without_mark
    ON hs_office.relation (type, anchorUuid, holderUuid, contactUuid)
    WHERE mark IS NULL;

CREATE UNIQUE INDEX unique_partner_relation
    ON hs_office.relation (type, anchorUuid, holderUuid)
    WHERE mark IS NULL AND type = 'PARTNER';

--//

-- =====================================================================================
--changeset marc.sandlus:hs-office-relation-debitor-anchor-CONSTRAINT-BY-TRIGGER endDelimiter:--//
-- -------------------------------------------------------------------------------------
alter table hs_office.relation
    drop constraint if exists relation_check_debitor_anchor_person;

drop function if exists hs_office.relation_check_debitor_anchor_partner cascade;

CREATE FUNCTION hs_office.relation_enforce_debitor_anchor_partner()
returns trigger as $$
declare
    countPartner integer;
begin
    if NEW.type = 'DEBITOR' then
        SELECT COUNT(*) FROM hs_office.relation r
            WHERE r.type = 'PARTNER' AND r.holderuuid = NEW.anchorUuid
            INTO countPartner;
        if countPartner < 1 then
            raise exception '[400] invalid debitor relation: anchor person must have a PARTNER relation';
        end if;
    end if;
    return NEW;
end;
$$ LANGUAGE plpgsql;;

create trigger relation_enforce_debitor_anchor_partner_tg before insert
    on hs_office.relation
    for each row execute function hs_office.relation_enforce_debitor_anchor_partner();

--//

-- ============================================================================
--changeset michael.hoennig:hs-office-relation-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.relation');
--//
