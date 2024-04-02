--liquibase formatted sql


-- ============================================================================
--changeset hs-office-debitor-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single debitor test record.
 */
create or replace procedure createHsOfficeDebitorTestData(
        withDebitorNumberSuffix numeric(5),
        forPartnerPersonName varchar,
        forBillingContactLabel varchar,
        withDefaultPrefix varchar
    )
    language plpgsql as $$
declare
    currentTask             varchar;
    idName                  varchar;
    relatedDebitorRelUuid   uuid;
    relatedBankAccountUuid  uuid;
begin
    idName := cleanIdentifier( forPartnerPersonName|| '-' || forBillingContactLabel);
    currentTask := 'creating debitor test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select debitorRel.uuid
            into relatedDebitorRelUuid
            from hs_office_relation debitorRel
            join hs_office_person person on person.uuid = debitorRel.holderUuid
                and (person.tradeName = forPartnerPersonName or person.familyName = forPartnerPersonName)
            where debitorRel.type = 'DEBITOR';

    select b.uuid
            into relatedBankAccountUuid
            from hs_office_bankaccount b
            where b.holder = forPartnerPersonName;

    raise notice 'creating test debitor: % (#%)', idName, withDebitorNumberSuffix;
    -- raise exception 'creating test debitor: (uuid=%, debitorRelUuid=%, debitornumbersuffix=%, billable=%, vatbusiness=%, vatreversecharge=%, refundbankaccountuuid=%, defaultprefix=%)',
    --    uuid_generate_v4(), relatedDebitorRelUuid, withDebitorNumberSuffix, true,     true,        false,            relatedBankAccountUuid, withDefaultPrefix;
    insert
        into hs_office_debitor (uuid,   debitorRelUuid,        debitornumbersuffix,     billable, vatbusiness, vatreversecharge, refundbankaccountuuid,  defaultprefix)
            values (uuid_generate_v4(), relatedDebitorRelUuid, withDebitorNumberSuffix, true,     true,        false,            relatedBankAccountUuid, withDefaultPrefix);
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
