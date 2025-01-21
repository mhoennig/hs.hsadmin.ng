--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-debitor-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single debitor test record.
 */
create or replace procedure hs_office.debitor_create_test_data(
        withDebitorNumberSuffix numeric(5),
        forPartnerPersonName varchar,
        forBillingContactCaption varchar,
        withDefaultPrefix varchar
    )
    language plpgsql as $$
declare
    idName                  varchar;
    relatedDebitorRelUuid   uuid;
    relatedBankAccountUuid  uuid;
begin
    idName := base.cleanIdentifier( forPartnerPersonName|| '-' || forBillingContactCaption);

    select debitorRel.uuid
            into relatedDebitorRelUuid
            from hs_office.relation debitorRel
            join hs_office.person person on person.uuid = debitorRel.holderUuid
                and (person.tradeName = forPartnerPersonName or person.familyName = forPartnerPersonName)
            where debitorRel.type = 'DEBITOR';

    select b.uuid
            into relatedBankAccountUuid
            from hs_office.bankaccount b
            where b.holder = forPartnerPersonName;

    raise notice 'creating test debitor: % (#%)', idName, withDebitorNumberSuffix;
    -- raise exception 'creating test debitor: (uuid=%, debitorRelUuid=%, debitornumbersuffix=%, billable=%, vatbusiness=%, vatreversecharge=%, refundbankaccountuuid=%, defaultprefix=%)',
    --    uuid_generate_v4(), relatedDebitorRelUuid, withDebitorNumberSuffix, true,     true,        false,            relatedBankAccountUuid, withDefaultPrefix;
    insert
        into hs_office.debitor (uuid,   debitorRelUuid,        debitornumbersuffix,     billable, vatbusiness, vatreversecharge, refundbankaccountuuid,  defaultprefix)
            values (uuid_generate_v4(), relatedDebitorRelUuid, withDebitorNumberSuffix, true,     true,        false,            relatedBankAccountUuid, withDefaultPrefix);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-debitor-TEST-DATA-GENERATION context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating debitor test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call hs_office.debitor_create_test_data(11, 'First GmbH', 'first contact', 'fir');
        call hs_office.debitor_create_test_data(12, 'Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', 'second contact', 'sec');
        call hs_office.debitor_create_test_data(13, 'Third OHG', 'third contact', 'thi');
    end;
$$;
--//
