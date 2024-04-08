--liquibase formatted sql

-- ============================================================================
--changeset test-package-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the given number of test packages for the given customer.
 */
create or replace procedure createPackageTestData(customerPrefix varchar, pacCount int)
    language plpgsql as $$
declare
    cust          test_customer;
    custAdminUser varchar;
    custAdminRole varchar;
    pacName       varchar;
    currentTask   varchar;
    pac           test_package;
begin
    select * from test_customer where test_customer.prefix = customerPrefix into cust;

    for t in 0..(pacCount-1)
        loop
            pacName = cust.prefix || to_char(t, 'fm00');
            currentTask = 'creating RBAC test package #' || pacName || ' for customer ' || cust.prefix || ' #' ||
                          cust.uuid;

            custAdminUser = 'customer-admin@' || cust.prefix || '.example.com';
            custAdminRole = 'test_customer#' || cust.prefix || ':ADMIN';
            call defineContext(currentTask, null, 'superuser-fran@hostsharing.net', custAdminRole);
            raise notice 'task: % by % as %', currentTask, custAdminUser, custAdminRole;

            insert
                into test_package (customerUuid, name, description)
                values (cust.uuid, pacName, 'Here you can add your own description of package ' || pacName || '.')
                returning * into pac;

            call grantRoleToUser(
                    getRoleId(testCustomerAdmin(cust)),
                    findRoleId(testPackageAdmin(pac)),
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
    cust test_customer;
begin
    for cust in (select * from test_customer)
        loop
            continue when cust.reference >= 90000; -- reserved for functional testing
            call createPackageTestData(cust.prefix, 3);
        end loop;

    commit;
end ;
$$;
--//


-- ============================================================================
--changeset test-package-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createPackageTestData('xxx', 3);
        call createPackageTestData('yyy', 3);
        call createPackageTestData('zzz', 3);
    end;
$$;
--//
