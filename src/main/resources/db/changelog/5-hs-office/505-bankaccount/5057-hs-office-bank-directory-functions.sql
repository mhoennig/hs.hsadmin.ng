--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-bank-directory-IBAN-BANK-CODE runOnChange:true endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Cuts the national bank code out of an IBAN, given the length it has in that country.
    The caller knows the length from the bank directory entry it matches against,
    because all bank codes of one country share it.
 */
create or replace function hs_office.iban_bank_code(iban varchar, bankCodeLength int)
    returns varchar
    language sql immutable strict as $$
        -- the BBAN starts at position 5, except in IT and SM, which prefix it with a CIN check character
        select substring(iban from case when left(iban, 2) in ('IT', 'SM') then 6 else 5 end for bankCodeLength);
    $$;
--//
