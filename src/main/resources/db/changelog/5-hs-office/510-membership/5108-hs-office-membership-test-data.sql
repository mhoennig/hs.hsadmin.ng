--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-membership-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single membership test record.
 */
create or replace procedure createHsOfficeMembershipTestData(
        forPartnerNumber numeric(5),
        newMemberNumberSuffix char(2) )
    language plpgsql as $$
declare
    relatedPartner          hs_office_partner;
begin
    select partner.* from hs_office_partner partner
                     where partner.partnerNumber = forPartnerNumber into relatedPartner;

    raise notice 'creating test Membership: M-% %', forPartnerNumber, newMemberNumberSuffix;
    raise notice '- using partner (%): %', relatedPartner.uuid, relatedPartner;
    insert
        into hs_office_membership (uuid, partneruuid, memberNumberSuffix, validity, status)
        values (uuid_generate_v4(), relatedPartner.uuid, newMemberNumberSuffix, daterange('20221001' , null, '[]'), 'ACTIVE');
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-membership-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating Membership test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call createHsOfficeMembershipTestData(10001, '01');
        call createHsOfficeMembershipTestData(10002, '02');
        call createHsOfficeMembershipTestData(10003, '03');
    end;
$$;
--//
