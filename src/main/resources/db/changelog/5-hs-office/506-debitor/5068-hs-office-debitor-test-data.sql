--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-debitor-TEST-DATA-GENERATOR runOnChange:true endDelimiter:--//
--validCheckSum: ANY
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
begin
    idName := base.cleanIdentifier( forPartnerPersonName|| '-' || forBillingContactCaption);

    select debitorRel.uuid
            into relatedDebitorRelUuid
            from hs_office.relation debitorRel
            join hs_office.person person on person.uuid = debitorRel.holderUuid
                and (person.tradeName = forPartnerPersonName or person.familyName = forPartnerPersonName)
            where debitorRel.type = 'DEBITOR';

    raise notice 'creating test debitor: % (#%)', idName, withDebitorNumberSuffix;
    insert
        into hs_office.debitor (uuid,   debitorRelUuid,        debitornumbersuffix,     billable, vatbusiness, vatreversecharge, defaultprefix)
            values (uuid_generate_v4(), relatedDebitorRelUuid, withDebitorNumberSuffix, true,     true,        false,            withDefaultPrefix);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-debitor-TEST-DATA-GENERATION context:!without-test-data endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating debitor test-data', null, 'hsh-alex_superuser', 'rbac.global#global:ADMIN');

        call hs_office.debitor_create_test_data(11, 'First GmbH', 'first contact', 'fir');
        call hs_office.debitor_create_test_data(12, 'Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', 'second contact', 'sec');
        call hs_office.debitor_create_test_data(13, 'Third OHG', 'third contact', 'thi');
    end;
$$;
--//
