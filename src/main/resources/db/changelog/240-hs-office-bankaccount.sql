
-- ============================================================================
--changeset hs-office-bankaccount-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_bankaccount
(
    uuid                uuid unique references RbacObject (uuid) initially deferred,
    holder              varchar(27) not null,
    iban                varchar(34) not null,
    bic                 varchar(11) not null
);
--//


-- ============================================================================
--changeset hs-office-bankaccount-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_bankaccount');
--//
