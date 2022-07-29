-- ============================================================================
--changeset hs-customer-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

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
            set local hsadminng.assumedRoles = '';
            set local hsadminng.currentTask to currentTask;

            -- When a new customer is created,
            custReference = 10000 + t;
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
