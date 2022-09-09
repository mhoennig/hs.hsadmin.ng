--liquibase formatted sql


-- ============================================================================
--changeset hs-admin-contact-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single contact test record.
 */
create or replace procedure createHsAdminContactTestData(contLabel varchar)
    language plpgsql as $$
declare
    currentTask   varchar;
    emailAddr varchar;
begin
    currentTask = 'creating RBAC test contact ' || contLabel;
    execute format('set local hsadminng.currentTask to %L', currentTask);

    emailAddr = 'customer-admin@' || cleanIdentifier(contLabel) || '.example.com';
    call defineContext(currentTask);
    perform createRbacUser(emailAddr);
    call defineContext(currentTask, null, emailAddr);

    raise notice 'creating test contact: %', contLabel;
    insert
        into hs_admin_contact (label, postaladdress, emailaddresses, phonenumbers)
        values (contLabel, $address$
Vorname Nachname
Straße Hnr
PLZ Stadt
$address$, emailAddr, '+49 123 1234567');
end; $$;
--//

/*
    Creates a range of test contact for mass data generation.
 */
create or replace procedure createTestContactTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
begin
    for t in startCount..endCount
        loop
            call createHsAdminContactTestData(intToVarChar(t, 4) || '#' || t);
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-admin-contact-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsAdminContactTestData('first contact');
        call createHsAdminContactTestData('second contact');
        call createHsAdminContactTestData('third contact');
        call createHsAdminContactTestData('forth contact');
    end;
$$;
--//
