--liquibase formatted sql

-- ============================================================================
--changeset hs-unixuser-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the given count of test unix users for a single package.
 */
create or replace procedure createUnixUserTestData( packageName varchar, unixUserCount int )
    language plpgsql as $$
declare
    pac         record;
    pacAdmin    varchar;
    currentTask varchar;
begin
    set hsadminng.currentUser to '';

    select p.uuid, p.name, c.prefix as custPrefix
        from package p
        join customer c on p.customeruuid = c.uuid
        where p.name = packageName
        into pac;

    for t in 0..(unixUserCount-1)
        loop
            currentTask = 'creating RBAC test unixuser #' || t || ' for package ' || pac.name || ' #' || pac.uuid;
            raise notice 'task: %', currentTask;
            pacAdmin = 'pac-admin-' || pac.name || '@' || pac.custPrefix || '.example.com';
            execute format('set local hsadminng.currentTask to %L', currentTask);
            execute format('set local hsadminng.currentUser to %L', pacAdmin);
            set local hsadminng.assumedRoles = '';

            insert
                into unixuser (name, packageUuid)
                values (pac.name || '-' || intToVarChar(t, 4), pac.uuid);
        end loop;
end; $$;

/*
    Creates a range of unix users for mass data generation.
 */
create or replace procedure createUnixUserTestData( unixUserPerPackage integer )
    language plpgsql as $$
declare
    pac         record;
    pacAdmin    varchar;
    currentTask varchar;
begin
    set hsadminng.currentUser to '';

    for pac in
        (select p.uuid, p.name
             from package p
                      join customer c on p.customeruuid = c.uuid
             where c.reference < 90000) -- reserved for functional testing
        loop
            call createUnixUserTestData(pac.name, 2);
            commit;
        end loop;

end; $$;
--//


-- ============================================================================
--changeset hs-unixuser-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createUnixUserTestData('xxx00', 2);
        call createUnixUserTestData('xxx01', 2);
        call createUnixUserTestData('xxx02', 2);

        call createUnixUserTestData('yyy00', 2);
        call createUnixUserTestData('yyy01', 2);
        call createUnixUserTestData('yyy02', 2);

        call createUnixUserTestData('zzz00', 2);
        call createUnixUserTestData('zzz01', 2);
        call createUnixUserTestData('zzz02', 2);
    end;
$$;
--//
