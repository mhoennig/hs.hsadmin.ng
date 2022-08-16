--liquibase formatted sql

-- ============================================================================
--changeset hs-package-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates test data for the package main table.
 */
create or replace procedure createPackageTestData(
    minCustomerReference integer, -- skip customers with reference below this
    doCommitAfterEach boolean -- only for mass data creation outside of Liquibase
)
    language plpgsql as $$
declare
    cust          customer;
    custAdminUser varchar;
    custAdminRole varchar;
    pacName       varchar;
    currentTask   varchar;
    pac           package;
begin
    set hsadminng.currentUser to '';

    for cust in (select * from customer)
        loop
            continue when cust.reference < minCustomerReference;

            for t in 0..2
                loop
                    pacName = cust.prefix || to_char(t, 'fm00');
                    currentTask = 'creating RBAC test package #' || pacName || ' for customer ' || cust.prefix || ' #' ||
                                  cust.uuid;

                    custAdminUser = 'admin@' || cust.prefix || '.example.com';
                    custAdminRole = 'customer#' || cust.prefix || '.admin';
                    execute format('set local hsadminng.currentUser to %L', custAdminUser);
                    execute format('set local hsadminng.assumedRoles to %L', custAdminRole);
                    execute format('set local hsadminng.currentTask to %L', currentTask);
                    raise notice 'task: % by % as %', currentTask, custAdminUser, custAdminRole;

                    insert
                        into package (customerUuid, name, description)
                        values (cust.uuid, pacName, 'Here can add your own description of package ' || pacName || '.')
                        returning * into pac;

                    call grantRoleToUser(
                        getRoleId(customerAdmin(cust), 'fail'),
                        findRoleId(packageAdmin(pac)),
                        createRbacUser(pacName || '@' || cust.prefix || '.example.com'),
                        true);

                end loop;
        end loop;

    if doCommitAfterEach then
        commit;
    end if;
end ;
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
