--liquibase formatted sql

-- ============================================================================
--changeset hs-package-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates test data for the package main table.
 */
create or replace procedure createPackageTestData(
    minCustomerReference integer,   -- skip customers with reference below this
    doCommitAfterEach boolean       -- only for mass data creation outside of Liquibase
)
    language plpgsql as $$
    declare
        cust        customer;
        pacName     varchar;
        currentTask varchar;
        custAdmin   varchar;
    begin
        set hsadminng.currentUser to '';

        for cust in (select * from customer)
            loop
                CONTINUE WHEN cust.reference < minCustomerReference;

                for t in 0..2
                    loop
                        pacName = cust.prefix || to_char(t, 'fm00');
                        currentTask = 'creating RBAC test package #' || pacName || ' for customer ' || cust.prefix || ' #' ||
                                      cust.uuid;
                        raise notice 'task: %', currentTask;

                        custAdmin = 'admin@' || cust.prefix || '.example.com';
                        set local hsadminng.currentUser to custAdmin;
                        set local hsadminng.assumedRoles = '';
                        set local hsadminng.currentTask to currentTask;

                        insert
                            into package (name, customerUuid)
                            values (pacName, cust.uuid);
                    end loop;
            end loop;

            if doCommitAfterEach then
                commit;
            end if;
    end;
$$;
--//


-- ============================================================================
--changeset hs-package-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createPackageTestData(0, false);
    end;
$$;
--//
