--liquibase formatted sql


-- ============================================================================
--changeset hs-admin-person-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single person test record.
 */
create or replace procedure createHsAdminPersonTestData(
        personType HsAdminPersonType,
        tradeName varchar,
        familyName varchar = null,
        givenName varchar = null
)
    language plpgsql as $$
declare
    fullName    varchar;
    currentTask varchar;
    emailAddr   varchar;
begin
    fullName := concat_ws(', ', personType, tradename, familyName, givenName);
    currentTask = 'creating RBAC test person ' || fullName;
    emailAddr = 'person-' || left(cleanIdentifier(fullName), 32) || '@example.com';
    call defineContext(currentTask);
    perform createRbacUser(emailAddr);
    call defineContext(currentTask, null, emailAddr);
    execute format('set local hsadminng.currentTask to %L', currentTask);

    raise notice 'creating test person: %', fullName;
    insert
        into hs_admin_person (persontype, tradename, givenname, familyname)
        values (personType, tradeName, givenName, familyName);
end; $$;
--//

/*
    Creates a range of test persons for mass data generation.
 */
create or replace procedure createTestPersonTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
begin
    for t in startCount..endCount
        loop
            call createHsAdminPersonTestData('LEGAL', intToVarChar(t, 4));
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-admin-person-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsAdminPersonTestData('LEGAL', 'first person');
        call createHsAdminPersonTestData('NATURAL', null, 'Peter', 'Smith');
        call createHsAdminPersonTestData('LEGAL', 'Rockshop e.K.', 'Sandra', 'Miller');
        call createHsAdminPersonTestData('SOLE_REPRESENTATION', 'Ostfriesische Kuhhandel OHG');
        call createHsAdminPersonTestData('JOINT_REPRESENTATION', 'Erbengemeinschaft Bessler', 'Mel', 'Bessler');
    end;
$$;
--//
