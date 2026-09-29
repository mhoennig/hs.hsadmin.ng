--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-bank-directory-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

-- BUNDESBANK is reserved for an import of the Bundesbank bank-code file, which must not be part of the repository;
-- EXISTING rows are derived once from the existing bank accounts, thus unconfirmed and without names.
create type hs_office.BankSource as enum ('BUNDESBANK', 'EXISTING', 'MANUAL', 'DEMO');

create cast (character varying as hs_office.BankSource) with inout as implicit;

-- Reference data without RBAC: banks by the country code and the national bank code of their IBANs.
create table hs_office.bank
(
    countryCode         char(2),
    bankCode            varchar(30),
    bic                 varchar(11),
    name                varchar(80),
    source              hs_office.BankSource not null,
    primary key (countryCode, bankCode)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-bank-directory-INITIAL-DATA endDelimiter:--//
-- ----------------------------------------------------------------------------

-- Until the Bundesbank file gets imported, the German banks of the existing bank accounts form the directory:
-- the most frequent valid German BIC wins over occasional typos; without a clear winner the bank code stays unknown.
with vote as (
    select substring(iban from 5 for 8) as bankCode, bic, count(*) as votes
        from (select upper(replace(iban, ' ', '')) as iban,
                     regexp_replace(upper(replace(bic, ' ', '')), '^(.{8})XXX$', '\1') as bic
                  from hs_office.bankaccount) as account
        where iban ~ '^DE[0-9]{20}$' and bic ~ '^[A-Z]{4}DE[A-Z0-9]{2}([A-Z0-9]{3})?$'
        group by bankCode, bic),
winner as (
    select bankCode, bic, rank() over (partition by bankCode order by votes desc) as rank
        from vote)
insert into hs_office.bank (countryCode, bankCode, bic, source)
    select 'DE', bankCode, min(bic), 'EXISTING'
        from winner
        where rank = 1
        group by bankCode
        having count(*) = 1;
--//
