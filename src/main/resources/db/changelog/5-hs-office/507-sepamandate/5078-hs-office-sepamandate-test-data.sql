--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-sepaMandate-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single sepaMandate test record.
 */
create or replace procedure createHsOfficeSepaMandateTestData(
        forPartnerNumber numeric(5),
        forDebitorSuffix char(2),
        forIban varchar,
        withReference varchar)
    language plpgsql as $$
declare
    relatedDebitor      hs_office.debitor;
    relatedBankAccount  hs_office.bankAccount;
begin
    select debitor.* into relatedDebitor
        from hs_office.debitor debitor
        join hs_office.relation debitorRel on debitorRel.uuid = debitor.debitorRelUuid
        join hs_office.relation partnerRel on partnerRel.holderUuid = debitorRel.anchorUuid
        join hs_office.partner partner on partner.partnerRelUuid = partnerRel.uuid
        where partner.partnerNumber = forPartnerNumber and debitor.debitorNumberSuffix = forDebitorSuffix;
    select b.* into relatedBankAccount
        from hs_office.bankAccount b where b.iban = forIban;

    raise notice 'creating test SEPA-mandate: %', forPartnerNumber::text || forDebitorSuffix::text;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    raise notice '- using bankAccount (%): %', relatedBankAccount.uuid, relatedBankAccount;
    insert
        into hs_office.sepamandate (uuid, debitoruuid, bankAccountuuid, reference, agreement, validity)
        values (uuid_generate_v4(), relatedDebitor.uuid, relatedBankAccount.uuid, withReference, '20220930', daterange('20221001' , '20261231', '[]'));
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-sepaMandate-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating SEPA-mandate test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call createHsOfficeSepaMandateTestData(10001, '11', 'DE02120300000000202051', 'ref-10001-11');
        call createHsOfficeSepaMandateTestData(10002, '12', 'DE02100500000054540402', 'ref-10002-12');
        call createHsOfficeSepaMandateTestData(10003, '13', 'DE02300209000106531065', 'ref-10003-13');
    end;
$$;
--//
