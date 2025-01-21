--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-domain-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the given count of test unix users for a single package.
 */
create or replace procedure rbactest.domain_create_test_data( packageName varchar, domainCount int )
    language plpgsql as $$
declare
    pac         record;
    pacAdmin    varchar;
begin
    select p.uuid, p.name, c.prefix as custPrefix
        from rbactest.package p
        join rbactest.customer c on p.customeruuid = c.uuid
        where p.name = packageName
        into pac;

    for t in 0..(domainCount-1)
        loop
            pacAdmin = 'pac-admin-' || pac.name || '@' || pac.custPrefix || '.example.com';
            call base.defineContext('creating RBAC test domain', null, pacAdmin, null);

            insert
                into rbactest.domain (name, packageUuid)
                values (pac.name || '-' || base.intToVarChar(t, 4), pac.uuid);
        end loop;
end; $$;

/*
    Creates a range of unix users for mass data generation.
 */
create or replace procedure rbactest.domain_create_test_data( domainPerPackage integer )
    language plpgsql as $$
declare
    pac         record;
begin
    for pac in
        (select p.uuid, p.name
             from rbactest.package p
                      join rbactest.customer c on p.customeruuid = c.uuid
             where c.reference < 90000) -- reserved for functional testing
        loop
            call rbactest.domain_create_test_data(pac.name, 2);
            commit;
        end loop;

end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-domain-TEST-DATA-GENERATION context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call rbactest.domain_create_test_data('xxx00', 2);
        call rbactest.domain_create_test_data('xxx01', 2);
        call rbactest.domain_create_test_data('xxx02', 2);

        call rbactest.domain_create_test_data('yyy00', 2);
        call rbactest.domain_create_test_data('yyy01', 2);
        call rbactest.domain_create_test_data('yyy02', 2);

        call rbactest.domain_create_test_data('zzz00', 2);
        call rbactest.domain_create_test_data('zzz01', 2);
        call rbactest.domain_create_test_data('zzz02', 2);
    end;
$$;
--//
