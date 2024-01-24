--liquibase formatted sql


-- ============================================================================
--changeset hs-office-coopSharesTransaction-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single coopSharesTransaction test record.
 */
create or replace procedure createHsOfficeCoopSharesTransactionTestData(
        givenPartnerNumber numeric,
        givenMemberNumberSuffix char(2)
)
    language plpgsql as $$
declare
    currentTask     varchar;
    membership      hs_office_membership;
begin
    currentTask = 'creating coopSharesTransaction test-data ' || givenPartnerNumber::text || givenMemberNumberSuffix;
    execute format('set local hsadminng.currentTask to %L', currentTask);

    call defineContext(currentTask);
    select m.uuid
        from hs_office_membership m
        join hs_office_partner p on p.uuid = m.partneruuid
        where p.partnerNumber = givenPartnerNumber
            and m.memberNumberSuffix = givenMemberNumberSuffix
        into membership;

    raise notice 'creating test coopSharesTransaction: %', givenPartnerNumber::text || givenMemberNumberSuffix;
    insert
        into hs_office_coopsharestransaction(uuid, membershipuuid, transactiontype, valuedate, sharecount, reference, comment)
        values
            (uuid_generate_v4(), membership.uuid, 'SUBSCRIPTION', '2010-03-15', 4, 'ref '||givenPartnerNumber::text || givenMemberNumberSuffix||'-1', 'initial subscription'),
            (uuid_generate_v4(), membership.uuid, 'CANCELLATION', '2021-09-01', -2, 'ref '||givenPartnerNumber::text || givenMemberNumberSuffix||'-2', 'cancelling some'),
            (uuid_generate_v4(), membership.uuid, 'ADJUSTMENT', '2022-10-20', 2, 'ref '||givenPartnerNumber::text || givenMemberNumberSuffix||'-3', 'some adjustment');
end; $$;
--//


-- ============================================================================
--changeset hs-office-coopSharesTransaction-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeCoopSharesTransactionTestData(10001, '01');
        call createHsOfficeCoopSharesTransactionTestData(10002, '02');
        call createHsOfficeCoopSharesTransactionTestData(10003, '03');
    end;
$$;
