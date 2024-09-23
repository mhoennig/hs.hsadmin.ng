--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-coopSharesTransaction-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single coopSharesTransaction test record.
 */
create or replace procedure hs_office.coopsharetx_create_test_data(
        givenPartnerNumber numeric,
        givenMemberNumberSuffix char(2)
)
    language plpgsql as $$
declare
    membership              hs_office.membership;
    subscriptionEntryUuid   uuid;
begin
    select m.uuid
        from hs_office.membership m
        join hs_office.partner p on p.uuid = m.partneruuid
        where p.partnerNumber = givenPartnerNumber
            and m.memberNumberSuffix = givenMemberNumberSuffix
        into membership;

    raise notice 'creating test coopSharesTransaction: %', givenPartnerNumber::text || givenMemberNumberSuffix;
    subscriptionEntryUuid := uuid_generate_v4();
    insert
        into hs_office.coopsharetx(uuid, membershipuuid, transactiontype, valuedate, sharecount, reference, comment, adjustedShareTxUuid)
        values
            (uuid_generate_v4(), membership.uuid, 'SUBSCRIPTION', '2010-03-15', 4, 'ref '||givenPartnerNumber::text || givenMemberNumberSuffix||'-1', 'initial subscription', null),
            (uuid_generate_v4(), membership.uuid, 'CANCELLATION', '2021-09-01', -2, 'ref '||givenPartnerNumber::text || givenMemberNumberSuffix||'-2', 'cancelling some', null),
            (subscriptionEntryUuid,       membership.uuid, 'SUBSCRIPTION',    '2022-10-20',  2, 'ref '||givenPartnerNumber::text || givenMemberNumberSuffix||'-3', 'some subscription', null),
            (uuid_generate_v4(), membership.uuid, 'ADJUSTMENT', '2022-10-21', -2, 'ref '||givenPartnerNumber::text || givenMemberNumberSuffix||'-4', 'some adjustment', subscriptionEntryUuid);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-coopSharesTransaction-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating coopSharesTransaction test-data');
        SET CONSTRAINTS ALL DEFERRED;

        call hs_office.coopsharetx_create_test_data(10001, '01');
        call hs_office.coopsharetx_create_test_data(10002, '02');
        call hs_office.coopsharetx_create_test_data(10003, '03');
    end;
$$;
