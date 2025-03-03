--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-membership-TEST-DATA-GENERATOR runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single membership test record.
 */
create or replace procedure hs_office.membership_create_test_data(
        forPartnerNumber numeric(5),
        newMemberNumberSuffix char(2),
        newValidity daterange,
        newStatus hs_office.HsOfficeMembershipStatus)
    language plpgsql as $$
declare
    relatedPartner          hs_office.partner;
begin
    select partner.* from hs_office.partner partner
                     where partner.partnerNumber = forPartnerNumber into relatedPartner;

    raise notice 'creating test Membership: M-% %', forPartnerNumber, newMemberNumberSuffix;
    raise notice '- using partner (%): %', relatedPartner.uuid, relatedPartner;
    if not exists (select true
                from hs_office.membership
                where partneruuid = relatedPartner.uuid and memberNumberSuffix = newMemberNumberSuffix)
    then
        insert into hs_office.membership (uuid, partneruuid, memberNumberSuffix, validity, status)
               values (uuid_generate_v4(), relatedPartner.uuid, newMemberNumberSuffix,
                       newValidity, newStatus);
    else
        update hs_office.membership
            set memberNumberSuffix = newMemberNumberSuffix,
                validity = newValidity,
                status = newStatus
            where partneruuid = relatedPartner.uuid;
    end if;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-membership-TEST-DATA-GENERATION runOnChange:true validCheckSum:ANY context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating Membership test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call hs_office.membership_create_test_data(10001, '01', daterange('20221001' , '20241231', '[)'), 'CANCELLED');
        call hs_office.membership_create_test_data(10002, '02', daterange('20221001' , '20251231', '[]'), 'CANCELLED');
        call hs_office.membership_create_test_data(10003, '03', daterange('20221001' , null, '[]'), 'ACTIVE');
    end;
$$;
--//
