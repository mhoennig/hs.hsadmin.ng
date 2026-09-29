--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-bank-directory-DEMO-DATA endDelimiter:--//
-- ----------------------------------------------------------------------------

-- Demo banks for test and demo databases, never for production; the German ones mirror src/test/resources/demodata/banks.csv.
-- Bank codes starting with 9 are never assigned by the Bundesbank, and a '0' in position 8 marks a BIC as unroutable.
insert into hs_office.bank (countryCode, bankCode, bic, name, source) values
    ('DE', '99010000', 'DEMSDEH0', 'Demostadt Sparkasse', 'DEMO'),
    ('DE', '99020000', 'DEMVDEH0', 'Volksbank Demoland eG', 'DEMO'),
    ('DE', '99030000', 'DEMRDEF0', 'Regionalbank Musterhausen', 'DEMO'),
    ('DE', '99040000', 'DEMDDEB0', 'Direktbank Beispiel', 'DEMO'),
    ('DE', '99050000', 'DEMGDEM0', 'Genossenschaftsbank Süd', 'DEMO'),
    ('DE', '99060000', 'DEMPDED0', 'Privatbank Probstheide', 'DEMO'),
    ('DE', '99070000', null, 'Zahlungsverkehrsstelle ohne BIC', 'DEMO'),
    ('AT', '99999', 'DEMOATW0', 'Demobank Österreich', 'DEMO')
on conflict (countryCode, bankCode) do update set bic = excluded.bic, name = excluded.name, source = excluded.source;
--//
