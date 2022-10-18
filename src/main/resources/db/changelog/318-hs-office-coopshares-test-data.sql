--liquibase formatted sql


-- ============================================================================
--changeset hs-office-coopSharesTransaction-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single coopSharesTransaction test record.
 */
create or replace procedure createHsOfficeCoopSharesTransactionTestData(givenMembershipNumber numeric)
    language plpgsql as $$
declare
    currentTask     varchar;
    membership      hs_office_membership;
begin
    currentTask = 'creating coopSharesTransaction test-data ' || givenMembershipNumber;
    execute format('set local hsadminng.currentTask to %L', currentTask);

    call defineContext(currentTask);
    select m.uuid from hs_office_membership m where m.memberNumber = givenMembershipNumber into membership;

    raise notice 'creating test coopSharesTransaction: %', givenMembershipNumber;
    insert
        into hs_office_coopsharestransaction(uuid, membershipuuid, transactiontype, valuedate, sharecount, reference, comment)
        values
            (uuid_generate_v4(), membership.uuid, 'SUBSCRIPTION', '2010-03-15', 2, 'ref '||givenMembershipNumber||'-1', 'initial subscription'),
            (uuid_generate_v4(), membership.uuid, 'SUBSCRIPTION', '2021-09-01', 24, 'ref '||givenMembershipNumber||'-2', 'subsscibing more'),
            (uuid_generate_v4(), membership.uuid, 'CANCELLATION', '2022-10-20', 12, 'ref '||givenMembershipNumber||'-3', 'cancelling some');
end; $$;
--//


-- ============================================================================
--changeset hs-office-coopSharesTransaction-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeCoopSharesTransactionTestData(10001);
        call createHsOfficeCoopSharesTransactionTestData(10002);
        call createHsOfficeCoopSharesTransactionTestData(10003);
    end;
$$;
