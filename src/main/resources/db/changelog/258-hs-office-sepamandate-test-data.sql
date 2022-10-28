--liquibase formatted sql


-- ============================================================================
--changeset hs-office-sepaMandate-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single sepaMandate test record.
 */
create or replace procedure createHsOfficeSepaMandateTestData( tradeNameAndHolderName varchar )
    language plpgsql as $$
declare
    currentTask         varchar;
    idName              varchar;
    relatedDebitor      hs_office_debitor;
    relatedBankAccount  hs_office_bankAccount;
begin
    idName := cleanIdentifier( tradeNameAndHolderName);
    currentTask := 'creating SEPA-mandate test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select debitor.* from hs_office_debitor debitor
                      join hs_office_partner parter on parter.uuid = debitor.partnerUuid
                      join hs_office_person person on person.uuid = parter.personUuid
                     where person.tradeName = tradeNameAndHolderName into relatedDebitor;
    select c.* from hs_office_bankAccount c where c.holder = tradeNameAndHolderName into relatedBankAccount;

    raise notice 'creating test SEPA-mandate: %', idName;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    raise notice '- using bankAccount (%): %', relatedBankAccount.uuid, relatedBankAccount;
    insert
        into hs_office_sepamandate (uuid, debitoruuid, bankAccountuuid, reference, agreement, validity)
        values (uuid_generate_v4(), relatedDebitor.uuid, relatedBankAccount.uuid, 'ref'||idName, '20220930', daterange('20221001' , '20261231', '[]'));
end; $$;
--//


-- ============================================================================
--changeset hs-office-sepaMandate-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeSepaMandateTestData('First GmbH');
        call createHsOfficeSepaMandateTestData('Second e.K.');
        call createHsOfficeSepaMandateTestData('Third OHG');
    end;
$$;
--//
