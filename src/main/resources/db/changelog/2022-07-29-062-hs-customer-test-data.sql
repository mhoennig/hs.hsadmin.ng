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
    Creates test data for the customer main table.
 */
create or replace procedure createCustomerTestData(
    startCount integer,         -- count of auto generated rows before the run
    endCount integer,           -- count of auto generated rows after the run
    doCommitAfterEach boolean   -- only for mass data creation outside of Liquibase
)
    language plpgsql as $$
declare
    currentTask   varchar;
    custReference integer;
    custRowId     uuid;
    custPrefix    varchar;
    custAdminName varchar;
begin
    set hsadminng.currentUser to '';

    for t in startCount..endCount
        loop
            currentTask = 'creating RBAC test customer #' || t;
            set local hsadminng.currentUser to 'mike@hostsharing.net';
            set local hsadminng.assumedRoles to 'global#hostsharing.admin';
            execute format('set local hsadminng.currentTask to %L', currentTask);

            -- When a new customer is created,
            custReference = testCustomerReference(t);
            custRowId = uuid_generate_v4();
            custPrefix = intToVarChar(t, 3);
            custAdminName = 'admin@' || custPrefix || '.example.com';

            raise notice 'creating customer %:%', custReference, custPrefix;
            insert
                into customer (reference, prefix, adminUserName)
                values (custReference, custPrefix, custAdminName);

            if doCommitAfterEach then
                commit;
            end if;

        end loop;

end; $$;
--//


-- ============================================================================
--changeset hs-customer-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createCustomerTestData(0, 2, false);
    end;
$$;
--//
