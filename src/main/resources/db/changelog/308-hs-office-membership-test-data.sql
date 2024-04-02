--liquibase formatted sql


-- ============================================================================
--changeset hs-office-membership-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single membership test record.
 */
create or replace procedure createHsOfficeMembershipTestData(
        forPartnerNumber numeric(5),
        newMemberNumberSuffix char(2) )
    language plpgsql as $$
declare
    currentTask             varchar;
    relatedPartner          hs_office_partner;
begin
    currentTask := 'creating Membership test-data ' ||
                    'P-' || forPartnerNumber::text ||
                    'M-...' || newMemberNumberSuffix;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select partner.* from hs_office_partner partner
                     where partner.partnerNumber = forPartnerNumber into relatedPartner;

    raise notice 'creating test Membership: M-% %', forPartnerNumber, newMemberNumberSuffix;
    raise notice '- using partner (%): %', relatedPartner.uuid, relatedPartner;
    insert
        into hs_office_membership (uuid, partneruuid, memberNumberSuffix, validity, reasonfortermination)
        values (uuid_generate_v4(), relatedPartner.uuid, newMemberNumberSuffix, daterange('20221001' , null, '[]'), 'NONE');
end; $$;
--//


-- ============================================================================
--changeset hs-office-membership-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeMembershipTestData(10001, '01');
        call createHsOfficeMembershipTestData(10002, '02');
        call createHsOfficeMembershipTestData(10003, '03');
    end;
$$;
--//
