
-- ============================================================================
--changeset michael.hoennig:hs-office-bankaccount-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_bankaccount
(
    uuid                uuid unique references rbac.object (uuid) initially deferred,
    version             int not null default 0,
    holder              varchar(64) not null,
    iban                varchar(34) not null,
    bic                 varchar(11) not null
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-bankaccount-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office_bankaccount');
--//
