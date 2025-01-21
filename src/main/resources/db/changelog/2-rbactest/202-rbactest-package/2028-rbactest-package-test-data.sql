--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:test-package-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the given number of test packages for the given customer.
 */
create or replace procedure rbactest.package_create_test_data(customerPrefix varchar, pacCount int)
    language plpgsql as $$
declare
    cust          rbactest.customer;
    custAdminUser varchar;
    custAdminRole varchar;
    pacName       varchar;
    pac           rbactest.package;
begin
    select * from rbactest.customer where rbactest.customer.prefix = customerPrefix into cust;

    for t in 0..(pacCount-1)
        loop
            pacName = cust.prefix || to_char(t, 'fm00');
            custAdminUser = 'customer-admin@' || cust.prefix || '.example.com';
            custAdminRole = 'rbactest.customer#' || cust.prefix || ':ADMIN';
            call base.defineContext('creating RBAC test package', null, 'superuser-fran@hostsharing.net', custAdminRole);

            insert
                into rbactest.package (customerUuid, name, description)
                values (cust.uuid, pacName, 'Here you can add your own description of package ' || pacName || '.')
                returning * into pac;

            call rbac.grantRoleToSubject(
                    rbac.getRoleId(rbactest.customer_ADMIN(cust)),
                    rbac.findRoleId(rbactest.package_ADMIN(pac)),
                    rbac.create_subject('pac-admin-' || pacName || '@' || cust.prefix || '.example.com'),
                    true);

        end loop;
end; $$;

/*
    Creates a range of test packages for mass data generation.
 */
create or replace procedure rbactest.package_create_test_data()
    language plpgsql as $$
declare
    cust rbactest.customer;
begin
    for cust in (select * from rbactest.customer)
        loop
            continue when cust.reference >= 90000; -- reserved for functional testing
            call rbactest.package_create_test_data(cust.prefix, 3);
        end loop;

    commit;
end ;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:test-package-TEST-DATA-GENERATION context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call rbactest.package_create_test_data('xxx', 3);
        call rbactest.package_create_test_data('yyy', 3);
        call rbactest.package_create_test_data('zzz', 3);
    end;
$$;
--//
