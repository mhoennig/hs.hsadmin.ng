--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-bankaccount-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single bankaccount test record.
 */
create or replace procedure createHsOfficeBankAccountTestData(givenHolder varchar, givenIBAN varchar, givenBIC varchar)
    language plpgsql as $$
declare
    emailAddr varchar;
begin
    emailAddr = 'bankaccount-admin@' || base.cleanIdentifier(givenHolder) || '.example.com';
    perform rbac.create_subject(emailAddr);
    call base.defineContext('creating bankaccount test-data', null, emailAddr);

    raise notice 'creating test bankaccount: %', givenHolder;
    insert
        into hs_office.bankaccount(uuid, holder, iban, bic)
        values (uuid_generate_v4(), givenHolder, givenIBAN, givenBIC);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-bankaccount-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating bankaccount test-data');

        -- IBANs+BICs taken from https://ibanvalidieren.de/beispiele.html
        call createHsOfficeBankAccountTestData('First GmbH', 'DE02120300000000202051', 'BYLADEM1001');
        call createHsOfficeBankAccountTestData('Peter Smith', 'DE02500105170137075030', 'INGDDEFF');
        call createHsOfficeBankAccountTestData('Second e.K.', 'DE02100500000054540402', 'BELADEBE');
        call createHsOfficeBankAccountTestData('Third OHG', 'DE02300209000106531065', 'CMCIDEDD');
        call createHsOfficeBankAccountTestData('Fourth eG', 'DE02200505501015871393', 'HASPDEHH');
        call createHsOfficeBankAccountTestData('Mel Bessler', 'DE02100100100006820101', 'PBNKDEFF');
        call createHsOfficeBankAccountTestData('Anita Bessler', 'DE02300606010002474689', 'DAAEDEDD');
        call createHsOfficeBankAccountTestData('Paul Winkler', 'DE02600501010002034304', 'SOLADEST600');
    end;
$$;
