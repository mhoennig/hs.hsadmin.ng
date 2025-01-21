--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-membership-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single membership test record.
 */
create or replace procedure hs_office.membership_create_test_data(
        forPartnerNumber numeric(5),
        newMemberNumberSuffix char(2) )
    language plpgsql as $$
declare
    relatedPartner          hs_office.partner;
begin
    select partner.* from hs_office.partner partner
                     where partner.partnerNumber = forPartnerNumber into relatedPartner;

    raise notice 'creating test Membership: M-% %', forPartnerNumber, newMemberNumberSuffix;
    raise notice '- using partner (%): %', relatedPartner.uuid, relatedPartner;
    insert
        into hs_office.membership (uuid, partneruuid, memberNumberSuffix, validity, status)
        values (uuid_generate_v4(), relatedPartner.uuid, newMemberNumberSuffix, daterange('20221001' , null, '[]'), 'ACTIVE');
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-membership-TEST-DATA-GENERATION context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating Membership test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call hs_office.membership_create_test_data(10001, '01');
        call hs_office.membership_create_test_data(10002, '02');
        call hs_office.membership_create_test_data(10003, '03');
    end;
$$;
--//
