--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:test-customer-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Generates a customer reference number for a given test data counter.
 */
create or replace function testCustomerReference(customerCount integer)
    returns integer
    returns null on null input
    language plpgsql as $$
begin
    return 10000 + customerCount;
end; $$;


/*
    Creates a single customer test record with dist.
 */
create or replace procedure createTestCustomerTestData(
    custReference integer,
    custPrefix    varchar
)
    language plpgsql as $$
declare
    custRowId     uuid;
    custAdminName varchar;
    custAdminUuid uuid;
    newCust       test_customer;
begin
    custRowId = uuid_generate_v4();
    custAdminName = 'customer-admin@' || custPrefix || '.example.com';
    custAdminUuid = rbac.create_subject(custAdminName);

    insert
        into test_customer (reference, prefix, adminUserName)
        values (custReference, custPrefix, custAdminName);

    select * into newCust
             from test_customer where reference=custReference;
    call rbac.grantRoleToSubject(
            rbac.getRoleId(testCustomerOwner(newCust)),
            rbac.getRoleId(testCustomerAdmin(newCust)),
            custAdminUuid,
            true);
end; $$;
--//

/*
    Creates a range of test customers for mass data generation.
 */
create or replace procedure createTestCustomerTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
begin
    for t in startCount..endCount
        loop
            call createTestCustomerTestData(testCustomerReference(t), base.intToVarChar(t, 3));
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:test-customer-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating RBAC test customer', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call createTestCustomerTestData(99901, 'xxx');
        call createTestCustomerTestData(99902, 'yyy');
        call createTestCustomerTestData(99903, 'zzz');
    end;
$$;
--//
