--liquibase formatted sql


-- ============================================================================
--changeset hs-office-person-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single person test record.
 */
create or replace procedure createHsOfficePersonTestData(
        newPersonType HsOfficePersonType,
        newTradeName varchar,
        newFamilyName varchar = null,
        newGivenName varchar = null
)
    language plpgsql as $$
declare
    fullName    varchar;
    currentTask varchar;
    emailAddr   varchar;
begin
    fullName := concat_ws(', ', newTradeName, newFamilyName, newGivenName);
    currentTask = 'creating person test-data ' || fullName;
    emailAddr = 'person-' || left(cleanIdentifier(fullName), 32) || '@example.com';
    call defineContext(currentTask);
    perform createRbacUser(emailAddr);
    call defineContext(currentTask, null, emailAddr);
    execute format('set local hsadminng.currentTask to %L', currentTask);

    raise notice 'creating test person: % by %', fullName, emailAddr;
    insert
        into hs_office_person (persontype, tradename, givenname, familyname)
        values (newPersonType, newTradeName, newGivenName, newFamilyName);
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
            call createHsOfficePersonTestData('LP', intToVarChar(t, 4));
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-office-person-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficePersonTestData('LP', 'Hostsharing eG');
        call createHsOfficePersonTestData('LP', 'First GmbH');
        call createHsOfficePersonTestData('NP', null, 'Firby', 'Susan');
        call createHsOfficePersonTestData('NP', null, 'Smith', 'Peter');
        call createHsOfficePersonTestData('NP', null, 'Tucker', 'Jack');
        call createHsOfficePersonTestData('NP', null, 'Fouler', 'Ellie');
        call createHsOfficePersonTestData('LP', 'Second e.K.', 'Smith', 'Peter');
        call createHsOfficePersonTestData('IF', 'Third OHG');
        call createHsOfficePersonTestData('LP', 'Fourth eG');
        call createHsOfficePersonTestData('UF', 'Erben Bessler', 'Mel', 'Bessler');
        call createHsOfficePersonTestData('NP', null, 'Bessler', 'Anita');
        call createHsOfficePersonTestData('NP', null, 'Bessler', 'Bert');
        call createHsOfficePersonTestData('NP', null, 'Winkler', 'Paul');
    end;
$$;
--//
