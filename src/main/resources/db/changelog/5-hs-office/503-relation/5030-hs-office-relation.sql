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
--changeset timotheus.pokorra:hs-office-relation-debitor-anchor-CONSTRAINT endDelimiter:--//
-- -------------------------------------------------------------------------------------

--
-- Name: relation_check_debitor_anchor_partner(RelationType, uuid); Type: FUNCTION; Schema: hs_office; Owner: test
--

CREATE FUNCTION hs_office.relation_check_debitor_anchor_partner(mytype hs_office.RelationType, debitoranchoruuid uuid) RETURNS boolean
    LANGUAGE plpgsql
    AS '
declare
    countPartner integer;
begin
    if mytype = ''DEBITOR'' then
        SELECT COUNT(*) FROM hs_office.relation r
            WHERE r.type = ''PARTNER'' AND r.holderuuid = debitoranchoruuid
            INTO countPartner;
        if countPartner < 1 then
            raise exception ''[400] invalid debitor relation: anchor person must have a PARTNER relation'';
        end if;
    end if;
    return true;
end; ';

ALTER TABLE hs_office.relation ADD CONSTRAINT check_debitor_anchor_person CHECK (hs_office.relation_check_debitor_anchor_partner(type, anchorUuid));

--//


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.relation');
--//
