--liquibase formatted sql


-- ============================================================================
--changeset hs-admin-partner-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single partner test record.
 */
create or replace procedure createHsAdminPartnerTestData( personTradeName varchar, contactLabel varchar )
    language plpgsql as $$
declare
    currentTask     varchar;
    idName          varchar;
    person          hs_admin_person;
    contact         hs_admin_contact;
begin
    idName := cleanIdentifier( personTradeName|| '-' || contactLabel);
    currentTask := 'creating RBAC test partner ' || idName;
    call defineContext(currentTask, null, 'alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select p.* from hs_admin_person p where p.tradeName = personTradeName into person;
    select c.* from hs_admin_contact c where c.label = contactLabel into contact;

    raise notice 'creating test partner: %', idName;
    raise notice '- using person (%): %', person.uuid, person;
    raise notice '- using contact (%): %', contact.uuid, contact;
    insert
        into hs_admin_partner (uuid, personuuid, contactuuid)
        values (uuid_generate_v4(), person.uuid, contact.uuid);
end; $$;
--//

/*
    Creates a range of test partner for mass data generation.
 */
create or replace procedure createTestContactTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
declare
    person hs_admin_person;
    contact hs_admin_contact;
begin
    for t in startCount..endCount
        loop
            select p.* from hs_admin_person p where tradeName = intToVarChar(t, 4) into person;
            select c.* from hs_admin_contact c where c.label = intToVarChar(t, 4) || '#' || t into contact;

            call createHsAdminPartnerTestData(person.uuid, contact.uuid);
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-admin-partner-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        -- call createHsAdminPartnerTestData('first person', 'first contact');

        call createHsAdminPartnerTestData('Rockshop e.K.', 'second contact');

        call createHsAdminPartnerTestData('Ostfriesische Kuhhandel OHG', 'third contact');
    end;
$$;
--//
