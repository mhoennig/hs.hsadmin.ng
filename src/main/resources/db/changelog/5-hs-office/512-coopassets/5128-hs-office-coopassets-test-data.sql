--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-coopAssetsTransaction-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single coopAssetsTransaction test record.
 */
create or replace procedure hs_office.coopassettx_create_test_data(
    givenPartnerNumber numeric,
    givenMemberNumberSuffix char(2)
    )
    language plpgsql as $$
declare
    membership              hs_office.membership;
    invalidLossTx           uuid;
    transferTx              uuid;
    adoptionTx              uuid;
begin
    select m.uuid
        from hs_office.membership m
                 join hs_office.partner p on p.uuid = m.partneruuid
        where p.partnerNumber = givenPartnerNumber
          and m.memberNumberSuffix = givenMemberNumberSuffix
        into membership;

    raise notice 'creating test coopAssetsTransaction: %', givenPartnerNumber || givenMemberNumberSuffix;
    invalidLossTx := uuid_generate_v4();
    transferTx := uuid_generate_v4();
    adoptionTx := uuid_generate_v4();
    insert
        into hs_office.coopassettx(uuid, membershipuuid, transactiontype, valuedate, assetvalue, reference, comment, revertedAssetTxUuid, assetAdoptionTxUuid)
        values
            (uuid_generate_v4(),  membership.uuid, 'DEPOSIT',    '2010-03-15',  320.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-1', 'initial deposit', null, null),
            (uuid_generate_v4(),  membership.uuid, 'DISBURSAL',  '2021-09-01', -128.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-2', 'partial disbursal', null, null),
            (invalidLossTx,       membership.uuid, 'DEPOSIT',    '2022-10-20',  128.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-3', 'some loss', null, null),
            (uuid_generate_v4(),  membership.uuid, 'REVERSAL', '2022-10-21', -128.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-3', 'some reversal', invalidLossTx, null),
            (transferTx,          membership.uuid, 'TRANSFER', '2023-12-31', -192.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-3', 'some reversal', null, adoptionTx),
            (adoptionTx,            membership.uuid, 'ADOPTION', '2023-12-31', 192.00, 'ref '||givenPartnerNumber || givenMemberNumberSuffix||'-3', 'some reversal', null, null);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-coopAssetsTransaction-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating coopAssetsTransaction test-data',
                                null,
                                'superuser-alex@hostsharing.net',
                                'rbac.global#global:ADMIN');
        SET CONSTRAINTS ALL DEFERRED;

        call hs_office.coopassettx_create_test_data(10001, '01');
        call hs_office.coopassettx_create_test_data(10002, '02');
        call hs_office.coopassettx_create_test_data(10003, '03');
    end;
$$;
