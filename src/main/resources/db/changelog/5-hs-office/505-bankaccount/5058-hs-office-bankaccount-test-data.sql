--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-bankaccount-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single bankaccount test record.
 */
create or replace procedure hs_office.bankaccount_create_test_data(givenHolder varchar, givenIBAN varchar, givenBIC varchar)
    language plpgsql as $$
declare
    emailAddr varchar;
begin
    emailAddr = 'bankaccount-admin@' || TRIM(SUBSTRING(base.cleanIdentifier(givenHolder) FOR 32)) || '.example.com';
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
        call hs_office.bankaccount_create_test_data('First GmbH', 'DE02120300000000202051', 'BYLADEM1001');
        call hs_office.bankaccount_create_test_data('Peter Smith', 'DE02500105170137075030', 'INGDDEFF');
        call hs_office.bankaccount_create_test_data('Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', 'DE02100500000054540402', 'BELADEBE');
        call hs_office.bankaccount_create_test_data('Third OHG', 'DE02300209000106531065', 'CMCIDEDD');
        call hs_office.bankaccount_create_test_data('Fourth eG', 'DE02200505501015871393', 'HASPDEHH');
        call hs_office.bankaccount_create_test_data('Mel Bessler', 'DE02100100100006820101', 'PBNKDEFF');
        call hs_office.bankaccount_create_test_data('Anita Bessler', 'DE02300606010002474689', 'DAAEDEDD');
        call hs_office.bankaccount_create_test_data('Paul Winkler', 'DE02600501010002034304', 'SOLADEST600');
    end;
$$;
