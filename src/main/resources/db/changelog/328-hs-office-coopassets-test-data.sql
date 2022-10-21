--liquibase formatted sql


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single coopAssetsTransaction test record.
 */
create or replace procedure createHsOfficeCoopAssetsTransactionTestData(givenMembershipNumber numeric)
    language plpgsql as $$
declare
    currentTask     varchar;
    membership      hs_office_membership;
begin
    currentTask = 'creating coopAssetsTransaction test-data ' || givenMembershipNumber;
    execute format('set local hsadminng.currentTask to %L', currentTask);

    call defineContext(currentTask);
    select m.uuid from hs_office_membership m where m.memberNumber = givenMembershipNumber into membership;

    raise notice 'creating test coopAssetsTransaction: %', givenMembershipNumber;
    insert
        into hs_office_coopassetstransaction(uuid, membershipuuid, transactiontype, valuedate, assetvalue, reference, comment)
        values
            (uuid_generate_v4(), membership.uuid, 'DEPOSIT', '2010-03-15', 320.00, 'ref '||givenMembershipNumber||'-1', 'initial deposit'),
            (uuid_generate_v4(), membership.uuid, 'DISBURSAL', '2021-09-01', -128.00, 'ref '||givenMembershipNumber||'-2', 'partial disbursal'),
            (uuid_generate_v4(), membership.uuid, 'ADJUSTMENT', '2022-10-20', 128.00, 'ref '||givenMembershipNumber||'-3', 'some adjustment');
end; $$;
--//


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeCoopAssetsTransactionTestData(10001);
        call createHsOfficeCoopAssetsTransactionTestData(10002);
        call createHsOfficeCoopAssetsTransactionTestData(10003);
    end;
$$;
