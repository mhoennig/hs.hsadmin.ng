--liquibase formatted sql


-- ============================================================================
--changeset hs-office-debitor-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single debitor test record.
 */
create or replace procedure createHsOfficeDebitorTestData(
        debitorNumberSuffix numeric(5),
        partnerTradeName varchar,
        billingContactLabel varchar,
        defaultPrefix varchar
    )
    language plpgsql as $$
declare
    currentTask             varchar;
    idName                  varchar;
    relatedPartner          hs_office_partner;
    relatedContact          hs_office_contact;
    relatedBankAccountUuid  uuid;
begin
    idName := cleanIdentifier( partnerTradeName|| '-' || billingContactLabel);
    currentTask := 'creating debitor test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select partner.* from hs_office_partner partner
               join hs_office_person person on person.uuid = partner.personUuid
               where person.tradeName = partnerTradeName into relatedPartner;
    select c.* from hs_office_contact c where c.label = billingContactLabel into relatedContact;
    select b.uuid from hs_office_bankaccount b where b.holder = partnerTradeName into relatedBankAccountUuid;

    raise notice 'creating test debitor: % (#%)', idName, debitorNumberSuffix;
    raise notice '- using partner (%): %', relatedPartner.uuid, relatedPartner;
    raise notice '- using billingContact (%): %', relatedContact.uuid, relatedContact;
    insert
        into hs_office_debitor (uuid, partneruuid, debitornumbersuffix, billable, billingcontactuuid, vatbusiness, vatreversecharge, refundbankaccountuuid, defaultprefix)
            values (uuid_generate_v4(), relatedPartner.uuid, debitorNumberSuffix, true, relatedContact.uuid, true, false, relatedBankAccountUuid, defaultPrefix);
end; $$;
--//


-- ============================================================================
--changeset hs-office-debitor-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeDebitorTestData(11, 'First GmbH', 'first contact', 'fir');
        call createHsOfficeDebitorTestData(12, 'Second e.K.', 'second contact', 'sec');
        call createHsOfficeDebitorTestData(13, 'Third OHG', 'third contact', 'thi');
    end;
$$;
--//
