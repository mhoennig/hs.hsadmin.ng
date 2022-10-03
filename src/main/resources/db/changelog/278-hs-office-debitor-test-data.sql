--liquibase formatted sql


-- ============================================================================
--changeset hs-office-debitor-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single debitor test record.
 */
create or replace procedure createHsOfficeDebitorTestData( partnerTradeName varchar, billingContactLabel varchar )
    language plpgsql as $$
declare
    currentTask         varchar;
    idName              varchar;
    relatedPartner      hs_office_partner;
    relatedContact      hs_office_contact;
    newDebitorNumber    numeric(6);
begin
    idName := cleanIdentifier( partnerTradeName|| '-' || billingContactLabel);
    currentTask := 'creating RBAC test debitor ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select partner.* from hs_office_partner partner
               join hs_office_person person on person.uuid = partner.personUuid
               where person.tradeName = partnerTradeName into relatedPartner;
    select c.* from hs_office_contact c where c.label = billingContactLabel into relatedContact;
    select coalesce(max(debitorNumber)+1, 10001) from hs_office_debitor into newDebitorNumber;

    raise notice 'creating test debitor: % (#%)', idName, newDebitorNumber;
    raise notice '- using partner (%): %', relatedPartner.uuid, relatedPartner;
    raise notice '- using billingContact (%): %', relatedContact.uuid, relatedContact;
    insert
        into hs_office_debitor (uuid, partneruuid, debitornumber, billingcontactuuid, vatbusiness)
        values (uuid_generate_v4(), relatedPartner.uuid, newDebitorNumber, relatedContact.uuid, true);
end; $$;
--//


-- ============================================================================
--changeset hs-office-debitor-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeDebitorTestData('First GmbH', 'first contact');
        call createHsOfficeDebitorTestData('Second e.K.', 'second contact');
        call createHsOfficeDebitorTestData('Third OHG', 'third contact');
    end;
$$;
--//
