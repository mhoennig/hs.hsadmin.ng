--liquibase formatted sql


-- ============================================================================
--changeset hs-office-membership-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single membership test record.
 */
create or replace procedure createHsOfficeMembershipTestData(
        forPartnerTradeName varchar,
        forMainDebitorNumberSuffix numeric,
        newMemberNumberSuffix char(2) )
    language plpgsql as $$
declare
    currentTask             varchar;
    idName                  varchar;
    relatedPartner          hs_office_partner;
    relatedDebitor          hs_office_debitor;
begin
    idName := cleanIdentifier( forPartnerTradeName || '#' || forMainDebitorNumberSuffix);
    currentTask := 'creating Membership test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select partner.* from hs_office_partner partner
                      join hs_office_person person on person.uuid = partner.personUuid
                     where person.tradeName = forPartnerTradeName into relatedPartner;
    select d.* from hs_office_debitor d
               where d.partneruuid = relatedPartner.uuid
                   and d.debitorNumberSuffix = forMainDebitorNumberSuffix
               into relatedDebitor;

    raise notice 'creating test Membership: %', idName;
    raise notice '- using partner (%): %', relatedPartner.uuid, relatedPartner;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    insert
        into hs_office_membership (uuid, partneruuid, maindebitoruuid, memberNumberSuffix, validity, reasonfortermination)
        values (uuid_generate_v4(), relatedPartner.uuid, relatedDebitor.uuid, newMemberNumberSuffix, daterange('20221001' , null, '[]'), 'NONE');
end; $$;
--//


-- ============================================================================
--changeset hs-office-membership-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeMembershipTestData('First GmbH', 11, '01');
        call createHsOfficeMembershipTestData('Second e.K.', 12, '02');
        call createHsOfficeMembershipTestData('Third OHG', 13, '03');
    end;
$$;
--//
