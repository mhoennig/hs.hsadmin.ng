--liquibase formatted sql


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single coopAssetsTransaction test record.
 */
create or replace procedure createHsOfficeCoopAssetsTransactionTestData(
    givenPartnerNumber numeric,
    givenMemberNumberSuffix char(2)
    )
    language plpgsql as $$
declare
    currentTask             varchar;
    membership              hs_office_membership;
    lossEntryUuid           uuid;
begin
    currentTask = 'creating coopAssetsTransaction test-data ' || givenPartnerNumber || givenMemberNumberSuffix;
    execute format('set local hsadminng.currentTask to %L', currentTask);
    SET CONSTRAINTS ALL DEFERRED;

    call defineContext(currentTask);
    select m.uuid
        from hs_office_membership m
                 join hs_office_partner p on p.uuid = m.partneruuid
        where p.partnerNumber = givenPartnerNumber
          and m.memberNumberSuffix = givenMemberNumberSuffix
        into membership;

    raise notice 'creating test coopAssetsTransaction: %', givenPartnerNumber || givenMemberNumberSuffix;
    lossEntryUuid := uuid_generate_v4();
    insert
        into hs_office_coopassetstransaction(uuid, membershipuuid, transactiontype, valuedate, assetvalue, reference, comment, adjustedAssetTxUuid)
        values
            (uuid_generate_v4(),  membership.uuid, 'DEPOSIT',    '2010-03-15',  320.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-1', 'initial deposit', null),
            (uuid_generate_v4(),  membership.uuid, 'DISBURSAL',  '2021-09-01', -128.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-2', 'partial disbursal', null),
            (lossEntryUuid,       membership.uuid, 'DEPOSIT',    '2022-10-20',  128.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-3', 'some loss', null),
            (uuid_generate_v4(),  membership.uuid, 'ADJUSTMENT', '2022-10-21', -128.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-3', 'some adjustment', lossEntryUuid);
end; $$;
--//


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeCoopAssetsTransactionTestData(10001, '01');
        call createHsOfficeCoopAssetsTransactionTestData(10002, '02');
        call createHsOfficeCoopAssetsTransactionTestData(10003, '03');
    end;
$$;
