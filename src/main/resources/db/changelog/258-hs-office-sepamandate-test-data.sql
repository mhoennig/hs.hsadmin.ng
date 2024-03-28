--liquibase formatted sql


-- ============================================================================
--changeset hs-office-sepaMandate-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single sepaMandate test record.
 */
create or replace procedure createHsOfficeSepaMandateTestData(
        forPartnerNumber numeric(5),
        forDebitorSuffix numeric(2),
        forIban varchar,
        withReference varchar)
    language plpgsql as $$
declare
    currentTask         varchar;
    relatedDebitor      hs_office_debitor;
    relatedBankAccount  hs_office_bankAccount;
begin
    currentTask := 'creating SEPA-mandate test-data ' || forPartnerNumber::text || forDebitorSuffix::text;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select debitor.* into relatedDebitor
        from hs_office_debitor debitor
        join hs_office_relation debitorRel on debitorRel.uuid = debitor.debitorRelUuid
        join hs_office_relation partnerRel on partnerRel.holderUuid = debitorRel.anchorUuid
        join hs_office_partner partner on partner.partnerRelUuid = partnerRel.uuid
        where partner.partnerNumber = forPartnerNumber and debitor.debitorNumberSuffix = forDebitorSuffix;
    select b.* into relatedBankAccount
        from hs_office_bankAccount b where b.iban = forIban;

    raise notice 'creating test SEPA-mandate: %', forPartnerNumber::text || forDebitorSuffix::text;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    raise notice '- using bankAccount (%): %', relatedBankAccount.uuid, relatedBankAccount;
    insert
        into hs_office_sepamandate (uuid, debitoruuid, bankAccountuuid, reference, agreement, validity)
        values (uuid_generate_v4(), relatedDebitor.uuid, relatedBankAccount.uuid, withReference, '20220930', daterange('20221001' , '20261231', '[]'));
end; $$;
--//


-- ============================================================================
--changeset hs-office-sepaMandate-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeSepaMandateTestData(10001, 11, 'DE02120300000000202051', 'ref-10001-11');
        call createHsOfficeSepaMandateTestData(10002, 12, 'DE02100500000054540402', 'ref-10002-12');
        call createHsOfficeSepaMandateTestData(10003, 13, 'DE02300209000106531065', 'ref-10003-13');
    end;
$$;
--//
