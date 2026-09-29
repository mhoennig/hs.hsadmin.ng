--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-sepaMandate-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single sepaMandate test record.
 */
create or replace procedure hs_office.sepamandate_create_test_data(
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
--changeset michael.hoennig:hs-office-sepaMandate-TEST-DATA-GENERATION context:!without-test-data endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating SEPA-mandate test-data', null, 'hsh-alex_superuser', 'rbac.global#global:ADMIN');

        call hs_office.sepamandate_create_test_data(10001, '11', 'DE02120300000000202051', 'ref-10001-11');
        call hs_office.sepamandate_create_test_data(10002, '12', 'DE02100500000054540402', 'ref-10002-12');
        call hs_office.sepamandate_create_test_data(10003, '13', 'DE02300209000106531065', 'ref-10003-13');
    end;
$$;
--//



-- ============================================================================
--changeset michael.hoennig:hs-office-bankaccount-TEST-DATA-DEMO-BANKS context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

-- The released bank account test-data changeset (5058) names real banks, which the bank directory check would reject
-- in test databases; this moves the test bank accounts to the demo banks (5056), same account numbers, new check digits.
-- It runs after the SEPA mandate test data, which looks the bank accounts up by their original IBANs.
do language plpgsql $$
    begin
        call base.defineContext('moving bankaccount test-data to demo banks');
        update hs_office.bankaccount set iban = 'DE34990200000000202051', bic = 'DEMVDEH0' where iban = 'DE02120300000000202051';
        update hs_office.bankaccount set iban = 'DE33990300000137075030', bic = 'DEMRDEF0' where iban = 'DE02500105170137075030';
        update hs_office.bankaccount set iban = 'DE09990100000054540402', bic = 'DEMSDEH0' where iban = 'DE02100500000054540402';
        update hs_office.bankaccount set iban = 'DE47990600000106531065', bic = 'DEMPDED0' where iban = 'DE02300209000106531065';
        update hs_office.bankaccount set iban = 'DE20990500001015871393', bic = 'DEMGDEM0' where iban = 'DE02200505501015871393';
        update hs_office.bankaccount set iban = 'DE45990500000006820101', bic = 'DEMGDEM0' where iban = 'DE02100100100006820101';
        update hs_office.bankaccount set iban = 'DE38990200000002474689', bic = 'DEMVDEH0' where iban = 'DE02300606010002474689';
        update hs_office.bankaccount set iban = 'DE52990100000002034304', bic = 'DEMSDEH0' where iban = 'DE02600501010002034304';
    end;
$$;
--//
