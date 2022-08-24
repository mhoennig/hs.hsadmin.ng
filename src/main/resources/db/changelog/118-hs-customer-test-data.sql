--liquibase formatted sql


-- ============================================================================
--changeset hs-customer-TEST-DATA-GENERATOR:1 endDelimiter:--//
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
create or replace procedure createCustomerTestData(
    custReference integer,
    custPrefix    varchar
)
    language plpgsql as $$
declare
    currentTask   varchar;
    custRowId     uuid;
    custAdminName varchar;
begin
    currentTask = 'creating RBAC test customer #' || custReference || '/' || custPrefix;
    set local hsadminng.currentUser to 'mike@hostsharing.net';
    set local hsadminng.assumedRoles to 'global#hostsharing.admin';
    execute format('set local hsadminng.currentTask to %L', currentTask);

    custRowId = uuid_generate_v4();
    custAdminName = 'customer-admin@' || custPrefix || '.example.com';

    raise notice 'creating customer %:%', custReference, custPrefix;
    insert
        into customer (reference, prefix, adminUserName)
        values (custReference, custPrefix, custAdminName);
end; $$;
--//

/*
    Creates a range of test customers for mass data generation.
 */
create or replace procedure createCustomerTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
begin
    set hsadminng.currentUser to '';

    for t in startCount..endCount
        loop
            call createCustomerTestData(testCustomerReference(t), intToVarChar(t, 3));
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-customer-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createCustomerTestData(99901, 'xxx');
        call createCustomerTestData(99902, 'yyy');
        call createCustomerTestData(99903, 'zzz');
    end;
$$;
--//
