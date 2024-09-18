--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-sepamandate-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_office.sepamandate
(
    uuid                uuid unique references rbac.object (uuid) initially deferred,
    version             int not null default 0,
    debitorUuid         uuid not null references hs_office.debitor(uuid),
    bankAccountUuid     uuid not null references hs_office.bankaccount(uuid),
    reference           varchar(96) not null,
    agreement           date not null,
    validity            daterange not null
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-sepamandate-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.sepamandate');
--//
