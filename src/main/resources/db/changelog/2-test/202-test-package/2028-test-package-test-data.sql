--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:test-package-TEST-DATA-GENERATOR endDelimiter:--//
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
    pac           test_package;
begin
    select * from test_customer where test_customer.prefix = customerPrefix into cust;

    for t in 0..(pacCount-1)
        loop
            pacName = cust.prefix || to_char(t, 'fm00');
            custAdminUser = 'customer-admin@' || cust.prefix || '.example.com';
            custAdminRole = 'test_customer#' || cust.prefix || ':ADMIN';
            call base.defineContext('creating RBAC test package', null, 'superuser-fran@hostsharing.net', custAdminRole);

            insert
                into test_package (customerUuid, name, description)
                values (cust.uuid, pacName, 'Here you can add your own description of package ' || pacName || '.')
                returning * into pac;

            call rbac.grantRoleToSubject(
                    rbac.getRoleId(testCustomerAdmin(cust)),
                    rbac.findRoleId(testPackageAdmin(pac)),
                    rbac.create_subject('pac-admin-' || pacName || '@' || cust.prefix || '.example.com'),
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
--changeset michael.hoennig:test-package-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createPackageTestData('xxx', 3);
        call createPackageTestData('yyy', 3);
        call createPackageTestData('zzz', 3);
    end;
$$;
--//
