--liquibase formatted sql

-- ============================================================================
--changeset hs-unixuser-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates test data for the package main table.
 */
create or replace procedure createUnixUserTestData(
    minCustomerReference integer, -- skip customers with reference below this
    unixUserPerPackage integer, -- create this many unix users for each package
    doCommitAfterEach boolean -- only for mass data creation outside of Liquibase
)
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
             where c.reference >= minCustomerReference)
        loop

            for t in 0..(unixUserPerPackage-1)
                loop
                    currentTask = 'creating RBAC test unixuser #' || t || ' for package ' || pac.name || ' #' || pac.uuid;
                    raise notice 'task: %', currentTask;
                    pacAdmin = 'admin@' || pac.name || '.example.com';
                    set local hsadminng.currentUser to 'mike@hostsharing.net'; -- TODO: use a package-admin
                    set local hsadminng.assumedRoles = '';
                    set local hsadminng.currentTask to currentTask;

                    insert
                        into unixuser (name, packageUuid)
                        values (pac.name || '-' || intToVarChar(t, 4), pac.uuid);

                    if doCommitAfterEach then
                        commit;
                    end if;
                end loop;
        end loop;

end;
$$;
--//


-- ============================================================================
--changeset hs-unixuser-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createUnixUserTestData(0, 2, false);
    end;
$$;
--//
