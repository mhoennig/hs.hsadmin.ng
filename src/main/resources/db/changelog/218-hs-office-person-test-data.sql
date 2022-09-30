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
    currentTask = 'creating RBAC test person ' || fullName;
    emailAddr = 'person-' || left(cleanIdentifier(fullName), 32) || '@example.com';
    call defineContext(currentTask);
    perform createRbacUser(emailAddr);
    call defineContext(currentTask, null, emailAddr);
    execute format('set local hsadminng.currentTask to %L', currentTask);

    raise notice 'creating test person: %', fullName;
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
            call createHsOfficePersonTestData('LEGAL', intToVarChar(t, 4));
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-office-person-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficePersonTestData('LEGAL', 'First GmbH');
        call createHsOfficePersonTestData('NATURAL', null, 'Smith', 'Peter');
        call createHsOfficePersonTestData('LEGAL', 'Second e.K.', 'Sandra', 'Miller');
        call createHsOfficePersonTestData('SOLE_REPRESENTATION', 'Third OHG');
        call createHsOfficePersonTestData('JOINT_REPRESENTATION', 'Erben Bessler', 'Mel', 'Bessler');
        call createHsOfficePersonTestData('NATURAL', null, 'Bessler', 'Anita');
        call createHsOfficePersonTestData('NATURAL', null, 'Winkler', 'Paul');
    end;
$$;
--//
