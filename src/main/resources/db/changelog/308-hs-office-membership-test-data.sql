--liquibase formatted sql


-- ============================================================================
--changeset hs-office-membership-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single membership test record.
 */
create or replace procedure createHsOfficeMembershipTestData( forPartnerTradeName varchar, forMainDebitorNumber numeric )
    language plpgsql as $$
declare
    currentTask     varchar;
    idName          varchar;
    relatedPartner  hs_office_partner;
    relatedDebitor  hs_office_debitor;
    newMemberNumber numeric;
begin
    idName := cleanIdentifier( forPartnerTradeName || '#' || forMainDebitorNumber);
    currentTask := 'creating Membership test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select partner.* from hs_office_partner partner
                      join hs_office_person person on person.uuid = partner.personUuid
                     where person.tradeName = forPartnerTradeName into relatedPartner;
    select d.* from hs_office_debitor d where d.debitorNumber = forMainDebitorNumber into relatedDebitor;
    select coalesce(max(memberNumber)+1, 10001) from hs_office_membership into newMemberNumber;

    raise notice 'creating test Membership: %', idName;
    raise notice '- using partner (%): %', relatedPartner.uuid, relatedPartner;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    insert
        into hs_office_membership (uuid, partneruuid, maindebitoruuid, membernumber, validity, reasonfortermination)
        values (uuid_generate_v4(), relatedPartner.uuid, relatedDebitor.uuid, newMemberNumber, daterange('20221001' , null, '[]'), 'NONE');
end; $$;
--//


-- ============================================================================
--changeset hs-office-membership-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeMembershipTestData('First GmbH', 10001);
        call createHsOfficeMembershipTestData('Second e.K.', 10002);
        call createHsOfficeMembershipTestData('Third OHG', 10003);
    end;
$$;
--//
