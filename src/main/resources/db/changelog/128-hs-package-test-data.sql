--liquibase formatted sql

-- ============================================================================
--changeset hs-package-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the given number of test packages for the given customer.
 */
create or replace procedure createPackageTestData(customerPrefix varchar, pacCount int)
    language plpgsql as $$
declare
    cust          customer;
    custAdminUser varchar;
    custAdminRole varchar;
    pacName       varchar;
    currentTask   varchar;
    pac           package;
begin
    select * from customer where customer.prefix = customerPrefix into cust;

    for t in 0..(pacCount-1)
        loop
            pacName = cust.prefix || to_char(t, 'fm00');
            currentTask = 'creating RBAC test package #' || pacName || ' for customer ' || cust.prefix || ' #' ||
                          cust.uuid;

            custAdminUser = 'customer-admin@' || cust.prefix || '.example.com';
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
                    createRbacUser('pac-admin-' || pacName || '@' || cust.prefix || '.example.com'),
                    true);

        end loop;
end; $$;

/*
    Creates a range of test packages for mass data generation.
 */
create or replace procedure createPackageTestData()
    language plpgsql as $$
declare
    cust customer;
begin
    set hsadminng.currentUser to '';

    for cust in (select * from customer)
        loop
            continue when cust.reference >= 90000; -- reserved for functional testing
            call createPackageTestData(cust.prefix, 3);
        end loop;

    commit;
end ;
$$;
--//


-- ============================================================================
--changeset hs-package-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createPackageTestData('xxx', 3);
        call createPackageTestData('yyy', 3);
        call createPackageTestData('zzz', 3);
    end;
$$;
--//
