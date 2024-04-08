--liquibase formatted sql

-- ============================================================================
--changeset hs-office-sepamandate-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_office_sepamandate
(
    uuid                uuid unique references RbacObject (uuid) initially deferred,
    version      int not null default 0,
    debitorUuid         uuid not null references hs_office_debitor(uuid),
    bankAccountUuid     uuid not null references hs_office_bankaccount(uuid),
    reference           varchar(96) not null,
    agreement           date not null,
    validity            daterange not null
);
--//


-- ============================================================================
--changeset hs-office-sepamandate-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_sepamandate');
--//
