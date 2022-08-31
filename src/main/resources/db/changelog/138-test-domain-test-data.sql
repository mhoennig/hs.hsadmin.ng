--liquibase formatted sql

-- ============================================================================
--changeset hs-domain-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the given count of test unix users for a single package.
 */
create or replace procedure createdomainTestData( packageName varchar, domainCount int )
    language plpgsql as $$
declare
    pac         record;
    pacAdmin    varchar;
    currentTask varchar;
begin
    select p.uuid, p.name, c.prefix as custPrefix
        from test_package p
        join test_customer c on p.customeruuid = c.uuid
        where p.name = packageName
        into pac;

    for t in 0..(domainCount-1)
        loop
            currentTask = 'creating RBAC test domain #' || t || ' for package ' || pac.name || ' #' || pac.uuid;
            raise notice 'task: %', currentTask;
            pacAdmin = 'pac-admin-' || pac.name || '@' || pac.custPrefix || '.example.com';
            call defineContext(currentTask, null, pacAdmin, null);

            insert
                into test_domain (name, packageUuid)
                values (pac.name || '-' || intToVarChar(t, 4), pac.uuid);
        end loop;
end; $$;

/*
    Creates a range of unix users for mass data generation.
 */
create or replace procedure createdomainTestData( domainPerPackage integer )
    language plpgsql as $$
declare
    pac         record;
    pacAdmin    varchar;
    currentTask varchar;
begin
    for pac in
        (select p.uuid, p.name
             from test_package p
                      join test_customer c on p.customeruuid = c.uuid
             where c.reference < 90000) -- reserved for functional testing
        loop
            call createdomainTestData(pac.name, 2);
            commit;
        end loop;

end; $$;
--//


-- ============================================================================
--changeset hs-domain-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createdomainTestData('xxx00', 2);
        call createdomainTestData('xxx01', 2);
        call createdomainTestData('xxx02', 2);

        call createdomainTestData('yyy00', 2);
        call createdomainTestData('yyy01', 2);
        call createdomainTestData('yyy02', 2);

        call createdomainTestData('zzz00', 2);
        call createdomainTestData('zzz01', 2);
        call createdomainTestData('zzz02', 2);
    end;
$$;
--//
