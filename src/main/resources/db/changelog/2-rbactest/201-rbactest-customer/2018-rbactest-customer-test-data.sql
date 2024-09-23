--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:test-customer-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Generates a customer reference number for a given test data counter.
 */
create or replace function rbactest.customer_create_test_data(customerCount integer)
    returns integer
    returns null on null input
    language plpgsql as $$
begin
    return 10000 + customerCount;
end; $$;


/*
    Creates a single customer test record with dist.
 */
create or replace procedure rbactest.customer_create_test_data(
    custReference integer,
    custPrefix    varchar
)
    language plpgsql as $$
declare
    custRowId     uuid;
    custAdminName varchar;
    custAdminUuid uuid;
    newCust       rbactest.customer;
begin
    custRowId = uuid_generate_v4();
    custAdminName = 'customer-admin@' || custPrefix || '.example.com';
    custAdminUuid = rbac.create_subject(custAdminName);

    insert
        into rbactest.customer (reference, prefix, adminUserName)
        values (custReference, custPrefix, custAdminName);

    select * into newCust
             from rbactest.customer where reference=custReference;
    call rbac.grantRoleToSubject(
            rbac.getRoleId(rbactest.customer_OWNER(newCust)),
            rbac.getRoleId(rbactest.customer_ADMIN(newCust)),
            custAdminUuid,
            true);
end; $$;
--//

/*
    Creates a range of test customers for mass data generation.
 */
create or replace procedure rbactest.customer_create_test_data(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
begin
    for t in startCount..endCount
        loop
            call rbactest.customer_create_test_data(rbactest.testCustomerReference(t), base.intToVarChar(t, 3));
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:test-customer-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating RBAC test customer', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call rbactest.customer_create_test_data(99901, 'xxx');
        call rbactest.customer_create_test_data(99902, 'yyy');
        call rbactest.customer_create_test_data(99903, 'zzz');
    end;
$$;
--//
