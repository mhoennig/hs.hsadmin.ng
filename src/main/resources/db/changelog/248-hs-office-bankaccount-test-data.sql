--liquibase formatted sql


-- ============================================================================
--changeset hs-office-bankaccount-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single bankaccount test record.
 */
create or replace procedure createHsOfficeBankAccountTestData(givenHolder varchar, givenIBAN varchar, givenBIC varchar)
    language plpgsql as $$
declare
    currentTask   varchar;
    emailAddr varchar;
begin
    currentTask = 'creating RBAC test bankaccount ' || givenHolder;
    execute format('set local hsadminng.currentTask to %L', currentTask);

    emailAddr = 'bankaccount-admin@' || cleanIdentifier(givenHolder) || '.example.com';
    call defineContext(currentTask);
    perform createRbacUser(emailAddr);
    call defineContext(currentTask, null, emailAddr);

    raise notice 'creating test bankaccount: %', givenHolder;
    insert
        into hs_office_bankaccount(uuid, holder, iban, bic)
        values (uuid_generate_v4(), givenHolder, givenIBAN, givenBIC);
end; $$;
--//


-- ============================================================================
--changeset hs-office-bankaccount-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        -- IBANs+BICs taken from https://ibanvalidieren.de/beispiele.html
        call createHsOfficeBankAccountTestData('First GmbH', 'DE02120300000000202051', 'BYLADEM1001');
        call createHsOfficeBankAccountTestData('Peter Smith', 'DE02500105170137075030', 'INGDDEFF');
        call createHsOfficeBankAccountTestData('Second e.K.', 'DE02100500000054540402', 'BELADEBE');
        call createHsOfficeBankAccountTestData('Third OHG', 'DE02300209000106531065', 'CMCIDEDD');
        call createHsOfficeBankAccountTestData('Fourth e.G.', 'DE02200505501015871393', 'HASPDEHH');
        call createHsOfficeBankAccountTestData('Mel Bessler', 'DE02100100100006820101', 'PBNKDEFF');
        call createHsOfficeBankAccountTestData('Anita Bessler', 'DE02300606010002474689', 'DAAEDEDD');
        call createHsOfficeBankAccountTestData('Paul Winkler', 'DE02600501010002034304', 'SOLADEST600');
    end;
$$;
