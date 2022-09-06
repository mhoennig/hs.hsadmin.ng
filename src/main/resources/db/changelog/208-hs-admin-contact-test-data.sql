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
    contRowId     uuid;
    contEmailAddr varchar;
begin
    currentTask = 'creating RBAC test contact ' || contLabel;
    call defineContext(currentTask, null, 'alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    -- contRowId = uuid_generate_v4();
    contEmailAddr = 'customer-admin@' || cleanIdentifier(contLabel) || '.example.com';

    raise notice 'creating test contact: %', contLabel;
    insert
        into hs_admin_contact (label, postaladdress, emailaddresses, phonenumbers)
        values (contLabel, $addr$
Vorname Nachname
Straße Hnr
PLZ Stadt
$addr$, contEmailAddr, '+49 123 1234567');
end; $$;
--//

/*
    Creates a range of test customers for mass data generation.
 */
create or replace procedure createTestCustomerTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
begin
    for t in startCount..endCount
        loop
            call createHsAdminContactTestData(intToVarChar(t, 4)|| ' ' || testCustomerReference(t));
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
    end;
$$;
--//
