--liquibase formatted sql


-- ============================================================================
--changeset test-customer-TEST-DATA-GENERATOR:1 endDelimiter:--//
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
    currentTask   varchar;
    custRowId     uuid;
    custAdminName varchar;
    custAdminUuid uuid;
    newCust       test_customer;
begin
    currentTask = 'creating RBAC test customer #' || custReference || '/' || custPrefix;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    custRowId = uuid_generate_v4();
    custAdminName = 'customer-admin@' || custPrefix || '.example.com';
    custAdminUuid = createRbacUser(custAdminName);

    insert
        into test_customer (reference, prefix, adminUserName)
        values (custReference, custPrefix, custAdminName);

    select * into newCust
             from test_customer where reference=custReference;
    call grantRoleToUser(
            getRoleId(testCustomerOwner(newCust)),
            getRoleId(testCustomerAdmin(newCust)),
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
            call createTestCustomerTestData(testCustomerReference(t), intToVarChar(t, 3));
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset test-customer-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createTestCustomerTestData(99901, 'xxx');
        call createTestCustomerTestData(99902, 'yyy');
        call createTestCustomerTestData(99903, 'zzz');
    end;
$$;
--//
